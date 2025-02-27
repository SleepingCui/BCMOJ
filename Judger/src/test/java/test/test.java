package test;
import org.bcmoj.judger.Judger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.io.File;

public class test {
    public static final Logger LOGGER = LoggerFactory.getLogger(test.class);
    public static void main(String[] args) {
        String r = String.valueOf(Judger.judge(new File("D:\\UserData\\Mxing\\Desktop\\aa.cpp"),new File("D:\\UserData\\Mxing\\Desktop\\in.txt"),new File("D:\\UserData\\Mxing\\Desktop\\out.txt"),500));
        LOGGER.info(r);
    }
}
