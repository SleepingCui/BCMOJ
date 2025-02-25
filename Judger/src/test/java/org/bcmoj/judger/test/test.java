package org.bcmoj.judger.test;
import org.bcmoj.judger.Judger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class test {
//    public static final Logger LOGGER = LogManager.getLogger(test.class);
    public static void main(String[] args) {
        System.out.println(Judger.judge(new File("Judger/src/test/test_resources/aa.cpp"), new File("Judger/src/test/test_resources/in.txt"), new File("Judger/src/test/test_resources/out.txt"),1000));

    }

}
