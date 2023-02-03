package cx.shapefile.utils.cx;

import java.io.File;
import java.io.IOException;

public class FileUtils
{
    /**
     文件不存在则创建文件，并创建不存在的目录
     */
    public static void creatEmptyDir(File file) throws IOException
    {
        if (!file.exists())
        {
            File dir = new File(file.getParent());
            dir.mkdirs();
            file.createNewFile();
        }
    }

}
