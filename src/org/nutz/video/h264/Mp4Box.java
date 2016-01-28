package org.nutz.video.h264;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 用最简单直接的方式,获取H264视频的帧偏移量
 * @author wendal
 */
public class Mp4Box {

	private DataInputStream dis;
	
	private int[] chunkOffsets;

	/**
	 * 第二个atom box必须是moov哦,否则会出错
	 */
	public void load(InputStream in) throws IOException {
		dis = new DataInputStream(new BufferedInputStream(in, 1024 * 1024));
        try {
            while (dis.available() > 0) {
                int size = dis.readInt();
                String typeName = readTypeName();
//                System.out.printf("typeName=%s, size=%d\n",typeName, size);
                if ("moov".endsWith(typeName)) {
                    readMoov(size);
                    break;
                } else {
                    dis.skipBytes(size - 8);
                    continue;
                }
            }
        }
        finally {
            try {
                dis.close();
            }
            catch (Throwable e) {}
        }
	}

	void readMoov(int moovSize) throws IOException {
		moovSize -= 8;
		while (moovSize > 0) {
			int size = dis.readInt();
			String typeName = readTypeName();
			if ("trak".equals(typeName)) {
				if (readTrak(size))
					return;
			} else {
				dis.skipBytes(size - 8);
			}
			moovSize -= size;
		}
	}

	boolean readTrak(int trakSize) throws IOException {
		trakSize -= 8;
		while (trakSize > 0) {
			int size = dis.readInt();
			String typeName = readTypeName();
			if ("tkhd".equals(typeName)) {
				dis.skipBytes(size - 8 - 8);
				int width = dis.readInt();
//				System.out.println("width=" + width);
				dis.skipBytes(4);
				if (width == 0) {
					dis.skipBytes(trakSize - size - 8);
					return false;
				}
				continue;
			}
			if (!"mdia".equals(typeName)) {
				dis.skipBytes(size - 8);
				trakSize -= size;
				continue;
			}
			trakSize -= size;
			int mdiaSize = size - 8;
			while (mdiaSize > 0) {
				size = dis.readInt();
				typeName = readTypeName();
				if (!"minf".equals(typeName)) {
					dis.skipBytes(size - 8);
					mdiaSize -= size;
					continue;
				}
				mdiaSize -= size;
				
				int minfSize = size - 8;
				while (minfSize > 0) {
					size = dis.readInt();
					typeName = readTypeName();
					if (!"stbl".equals(typeName)) {
						dis.skipBytes(size - 8);
						minfSize -= size;
						continue;
					}
					minfSize -= size;
					
					int stblSize = size - 8;
					while (stblSize > 0) {
						size = dis.readInt();
						typeName = readTypeName();
						if (!"stco".equals(typeName)) {
							dis.skipBytes(size - 8);
							stblSize -= size;
							continue;
						}
						dis.skipBytes(4);
						int count = dis.readInt();
						chunkOffsets = new int[count];
						for (int i = 0; i < count; i++) {
							chunkOffsets[i] = dis.readInt();
//							System.out.println("> " + chunkOffsets[i]);
						}
						trakSize -= size;
						return true;
					}
				}
			}
		}
		return false;
	}
	
	String readTypeName() throws IOException {
		byte[] buf = new byte[4];
		dis.readFully(buf);
		String str = new String(buf);
//		System.out.println("TypeName = " + str);
		return str;
	}

	public int[] getChunkOffsets() {
        return chunkOffsets;
    }
	
//	public static final synchronized void main(String _你妹[]) throws Throwable {
//        FileInputStream in = new FileInputStream("H:/X2.mp4");
//        new Mp4Box().load(in);
//    }
}