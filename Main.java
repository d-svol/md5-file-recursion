public class Main {
    public static void main(String[] args) throws InterruptedException {
        String fileDirectory = args[0];
        int treadsAmount = Integer.parseInt(args[1]);
        MD5 first = new MD5(fileDirectory, treadsAmount);
        first.printMd5DirTree();
        System.out.println("Done");
    }
}
