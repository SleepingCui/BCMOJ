import org.bcmoj.judgeserver.JudgeServer;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        String cppFile = (String) System.getProperties().get("cppFile");
        String inFile = (String) System.getProperties().get("inFile");
        String outFile = (String) System.getProperties().get("outFile");
        Integer timeLimit = (Integer) System.getProperties().get("timeLimit");
        Integer checkpoints = (Integer) System.getProperties().get("checkpoints");
        if (cppFile == null || inFile == null || outFile == null || timeLimit == null || checkpoints == null) {
            printHelp();
            return;
        }

        String logo = """
                 ____   ____ __  __  ___      _       _           _                \s
                | __ ) / ___|  \\/  |/ _ \\    | |     | |_   _  __| | __ _  ___ _ __\s
                |  _ \\| |   | |\\/| | | | |_  | |  _  | | | | |/ _` |/ _` |/ _ \\ '__|
                | |_) | |___| |  | | |_| | |_| | | |_| | |_| | (_| | (_| |  __/ |  \s
                |____/ \\____|_|  |_|\\___/ \\___/   \\___/ \\__,_|\\__,_|\\__, |\\___|_|  \s
                                                                    |___/          \s""";
        System.out.println(logo);
        System.out.println("            Developed by SleepingCui   ver1.0-SNAPSHOT \n");
        JudgeServer.JServer(new File(cppFile), new File(inFile), new File(outFile), timeLimit, checkpoints);
//        JudgeServer.JServer(new File("D:\\UserData\\Mxing\\Desktop\\aa.cpp"), new File("D:\\UserData\\Mxing\\Desktop\\in.txt"), new File("D:\\UserData\\Mxing\\Desktop\\out.txt"), 100, 4);
 }
    private static void printHelp() {
        System.out.println("Usage: -DcppFile=<cppFile> -DinFile=<inFile> -DoutFile=<outFile> -DtimeLimit=<timeLimit> -Dcheckpoints=<checkpoints>");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -DcppFile=<cppFile>       Path to the C++ source file to be judged.");
        System.out.println("  -DinFile=<inFile>         Path to the input file for the program.");
        System.out.println("  -DoutFile=<outFile>       Path to the output file where the program's output will be stored.");
        System.out.println("  -DtimeLimit=<timeLimit>   Time limit for the program execution in milliseconds.");
        System.out.println("  -Dcheckpoints=<checkpoints> Number of checkpoints to be used during judging.");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -DcppFile=program.cpp -DinFile=input.txt -DoutFile=output.txt -DtimeLimit=1000 -Dcheckpoints=5 -jar judger.jar");
    }
}