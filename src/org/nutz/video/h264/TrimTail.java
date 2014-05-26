package org.nutz.video.h264;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.nutz.lang.Streams;

/**
 * 一个由ffmpeg生成的Mp4文件,通过qt-faststart处理后,尾部有残留的无用数据,删除之
 * @author wendal
 *
 */
public class TrimTail {

    public static boolean trimTail(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        try {
            boolean getMdat = false;
            boolean getMoov = false;
            byte[] buf = new byte[4];
            int offset = 0;
            while (fis.available() > 0) {
                int size = dis.readInt();
                offset += size;
                System.out.println("size="+size);
                System.out.println("offset now="+ offset);
                dis.readFully(buf);
                if ("moov".equals(new String(buf))) {
                    System.out.println(">>found moov");
                    getMoov = true;
                    if (getMdat)
                        break;
                } else if ("mdat".equals(new String(buf))) {
                    System.out.println(">>found mdat");
                    getMdat = true;
                    if (getMoov)
                        break;
                } else {
                    System.out.println("found " + new String(buf));
                }
                dis.skipBytes(size - 8);
            }
            dis.close();
            
            if (getMdat && getMoov) {
                System.out.println("Offset = " + offset);
                RandomAccessFile raf = new RandomAccessFile(f, "rws");
                raf.setLength(offset);
                raf.close();
                System.out.println(f.length());
                return true;
            }
        }
        finally {
            Streams.safeClose(fis);
        }
        return false;
    }
    
    public static void main(String[] args) throws Throwable {
        trimTail(new File("H:/X2.mp4"));
    }
}
