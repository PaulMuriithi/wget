package com.github.axet.wget;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.DownloadRetry;

class DirectMultipart {

    String target = null;

    DownloadInfo info;

    Runnable notify;

    Boolean stop;

    static public final int THREAD_COUNT = 3;

    /**
     * 
     * @param info
     *            download file information
     * @param target
     *            target file
     * @param stop
     *            multithread stop command
     * @param notify
     *            progress notify call
     */
    public DirectMultipart(DownloadInfo info, String target, Boolean stop, Runnable notify) {
        this.target = target;
        this.info = info;
        this.notify = notify;
        this.stop = stop;
    }

    boolean stop() {
        synchronized (stop) {
            return stop;
        }
    }

    void download() {
        try {
            RandomAccessFile fos = null;

            try {
                URL url = info.getSource();

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(WGet.CONNECT_TIMEOUT);
                conn.setReadTimeout(WGet.READ_TIMEOUT);

                File f = new File(target);
                info.setCount(FileUtils.sizeOf(f));

                fos = new RandomAccessFile(f, "rw");

                if (info.getCount() > 0) {
                    conn.setRequestProperty("Range", "bytes=" + info.getCount() + "-");
                    fos.seek(info.getCount());
                }

                byte[] bytes = new byte[WGet.BUF_SIZE];
                int read = 0;

                BufferedInputStream binaryreader = new BufferedInputStream(conn.getInputStream());

                while (!stop() && (read = binaryreader.read(bytes)) > 0) {

                    info.setCount(info.getCount() + read);
                    fos.write(bytes, 0, read);

                    notify.run();
                }

                binaryreader.close();
            } finally {
                if (fos != null)
                    fos.close();
            }
        } catch (IOException e) {
            throw new DownloadRetry(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
