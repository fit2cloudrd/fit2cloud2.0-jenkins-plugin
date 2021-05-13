import hudson.FilePath;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.*;

public class TestTar {

    @Test
    public void testFilePath() throws IOException, InterruptedException {
        FilePath filePath = new FilePath(new File("D:/tmp/argo-demo"));
        filePath.tar(new FileOutputStream("D:/tmp/argo.tar"), new DirScanner() {
            @Override
            public void scan(File dir, FileVisitor visitor) throws IOException {

                System.out.println(dir.getAbsolutePath());
            }
        });
    }

    @Test
    public void testTar() throws IOException {
        tar(new File("D:\\tmp\\argo-demo"));
    }

    public static void tar(File source){
        FileOutputStream out = null;
        TarArchiveOutputStream tarOut = null;

        String parentPath = source.getParent();
        File dest = new File(parentPath + "/" + source.getName()  + ".tar");
        try{
            out = new FileOutputStream(dest);
            tarOut = new TarArchiveOutputStream(out);
            //解决文件名过长
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            for (File file : source.listFiles()) {
                tarPack(file, tarOut,"");
            }
            tarOut.flush();
            tarOut.close();
        }catch (Exception e) {
            e.printStackTrace();
        }finally{
            try{
                if(out != null){
                    out.close();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
            try{
                if(tarOut != null){
                    tarOut.close();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void tarPack(File source,TarArchiveOutputStream tarOut,String parentPath){
        if(source.isDirectory()){
            tarDir(source,tarOut,parentPath);
        } else if (source.isFile()) {
            tarFile(source, tarOut, parentPath);
        }
    }

    public static void tarFile(File source,TarArchiveOutputStream tarOut,String parentPath){
        TarArchiveEntry entry = new TarArchiveEntry(parentPath + source.getName());
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        try {
            entry.setSize(source.length());
            tarOut.putArchiveEntry(entry);
            fis = new FileInputStream(source);
            bis = new BufferedInputStream(fis);
            IOUtils.copy(bis, tarOut);
            bis.close();
            tarOut.closeArchiveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                if(bis != null){
                    bis.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            try {
                if(fis != null){
                    fis.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    public static void tarDir(File sourceDir,TarArchiveOutputStream tarOut,String parentPath){
        //归档空目录
        if(sourceDir.listFiles().length < 1){
            TarArchiveEntry entry = new TarArchiveEntry(parentPath + sourceDir.getName() + "/");
            try {
                tarOut.putArchiveEntry(entry);
                tarOut.closeArchiveEntry();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //递归 归档
        for (File file : sourceDir.listFiles()) {
            tarPack(file, tarOut,parentPath + sourceDir.getName() + "/");
        }
    }
}
