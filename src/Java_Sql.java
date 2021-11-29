import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.Scanner;


public class Java_Sql {
    static Connection connection;

    //where we will execute everything
    public static void main(String[] args) {
        MyDatabase myDataBase = new MyDatabase();
        doStuff(myDataBase);

        System.out.println("Exiting...");
    }

    public static void doStuff(MyDatabase db) {

        String name = "Snowball Grottobow";
        String link = "bz4bnJ77um";
        try {
            Scanner sc = new Scanner(System.in);
            System.out.println("Give me a Users name: ");
            String maybeName = sc.nextLine();
            System.out.println("Give me a Videos link");
            String maybeLink = sc.nextLine();

            if (maybeName.length() > 0)
                name = maybeName;
            if (maybeLink.length() > 0)
                link = maybeLink;
            sc.close();
        } catch (Exception e) {
            System.out.println("Using defaults, loser.");
        }
        int id = db.getElfId(name);
        db.getBills(id, name);
        db.getVideoInfo(link);
        db.getLinks(name);
        db.oneViewer(true);
        db.buildRunTimeTable();
        db.getTopFive();


    }


}

class MyDatabase {
    //this class will create connection to sql
    private Connection connection;

    private static int viewsIndex = 1; //this index will be used as the key for the views table
    private static int videosIndex = 1;//this index will be used as the key for the videos table
    private static int runIndex = 1;//this index will be used as the key for the eunTime table

    private final String accountsTXT = "accounts.txt";
    private final String videosTXT = "videos.txt";
    private final String viewsTXT = "views.txt";


    //constructor
    public MyDatabase() {
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            // creates an in-memory database
            connection = DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb", "SA", "");

            createTables();
            readInData(viewsTXT);
            readInData(accountsTXT);
            readInData(videosTXT);
        } catch (ClassNotFoundException e) {
            e.printStackTrace(System.out);
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }


    //this method will implement all of the empty tables generated in sql
    public void createTables() {

        try {

            //this table will be for the views.txt file
            String viewed = "create table viewed ("
                    + " index integer primary key,"
                    + " accountID integer,"
                    + " viewerName VARCHAR(1000),"
                    + " link VARCHAR(10),"
                    + " whenAttribute integer,"
                    + ")";

            connection.createStatement().executeUpdate(viewed);

            //this table will be for the accounts.txt file
            String accounts = "create table accounts ("
                    + " billId integer,"
                    + " accountID integer,"
                    + " billingAddress VARCHAR(1000),"
                    + " amount integer,"
                    + " primary key(billId),"
                    + ")";

            connection.createStatement().executeUpdate(accounts);

            //this table will be for the videos.txt file
            String videos = "create table videos ("
                    + " index integer primary key,"
                    + " link VARCHAR(1000),"
                    + " creatorName VARCHAR(1000),"
                    + " videoName VARCHAR(1000),"
                    + " duration integer,"
                    + ")";

            connection.createStatement().executeUpdate(videos);

            //this table will be for the internal runtime table file
            String runTimeTable = "create table runTimeTable ("
                    + " index integer primary key,"
                    + " link VARCHAR(1000),"
                    + "videoName VARCHAR(1000),"
                    + " numViews integer,"
                    + " duration integer,"
                    + " runTime integer,"
                    + ")";

            connection.createStatement().executeUpdate(runTimeTable);


        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }

    }

    public void readInData(String fileName) {
        BufferedReader in = null;

        try {
            in = new BufferedReader((new FileReader(fileName)));

            // throw away the first line - the header
            in.readLine();

            // pre-load loop
            String line = in.readLine();
            while (line != null) {
                // split naively on commas
                // good enough for this dataset!
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    if (fileName.equals(viewsTXT)) {
                        insertViewedTable(parts[0], parts[1], parts[2], parts[3]);
                    } else if (fileName.equals(videosTXT)) {
                        insertVideosTable(parts[2], parts[0], parts[1], parts[3]); //i was attempting to use the link as the primary key alphabetically however i was not able to (therefore the third col is being inserted first)
                    } else if (fileName.equals(accountsTXT)) {
                        insertAccountsTable1(parts[1], parts[0], parts[2], parts[3]); //here the bill Id will be used as the key as it is numerical and distinct and starts from 1
                    }
                }
                // get next line
                line = in.readLine();
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //this method will insert in viewed sql table
    public void insertViewedTable(String id, String name, String link, String whenAttribute) {

        try {

            PreparedStatement addAccount = connection.prepareStatement(
                    "insert into viewed (index, accountID, viewerName, link, whenAttribute)"
                            + " values (?, ?, ?, ?, ?);"
            );

            addAccount.setInt(1, viewsIndex);
            addAccount.setInt(2, Integer.parseInt(id));
            addAccount.setString(3, name);
            addAccount.setString(4, link);
            addAccount.setInt(5, Integer.parseInt(whenAttribute));


            addAccount.executeUpdate();
            viewsIndex++;

            addAccount.close();

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    //this method will insert in videos sql table
    public void insertVideosTable(String link, String creatorName, String videoName, String duration) {

        try {
            PreparedStatement addAccount = connection.prepareStatement(
                    "insert into videos (index, link, creatorName, videoName, duration) values (?, ?, ?, ?, ?);"
            );

            addAccount.setInt(1, videosIndex);
            addAccount.setString(2, link);
            addAccount.setString(3, creatorName);
            addAccount.setString(4, videoName);
            addAccount.setInt(5, Integer.parseInt(duration));

            addAccount.executeUpdate();
            videosIndex++;
            addAccount.close();

        } catch (SQLException e) {
            System.out.println("Error in " + link + " " + videoName);
            e.printStackTrace(System.out);
        }
    }


    //this method will insert in accounts sql table
    public void insertAccountsTable1(String billId, String accountID, String billingAddress, String amount) {

        int aID = -1;


        try {
            PreparedStatement pstmt = connection.prepareStatement(
                    "Select billId From accounts where billId = ?;"
            );
            pstmt.setInt(1, Integer.parseInt(billId));

            ResultSet resultSet = pstmt.executeQuery();

            if (resultSet.next()) {
                aID = resultSet.getInt("billId");
                System.out.println(aID);
            } else {
                PreparedStatement addAccount = connection.prepareStatement(
                        "insert into accounts (billId, accountID, billingAddress, amount) values (?, ?, ?, ?);"
                );

                addAccount.setInt(1, Integer.parseInt(billId));
                addAccount.setInt(2, Integer.parseInt(accountID));
                addAccount.setString(3, billingAddress);
                addAccount.setInt(4, Integer.parseInt(amount));
                addAccount.executeUpdate();

                addAccount.close();

                resultSet.close();
            }
            pstmt.close();
        } catch (SQLException e) {
            System.out.println("Error in " + billId + " " + billingAddress);
            e.printStackTrace(System.out);
        }
    }


    //this method will run a query in sql to find name in the accounts table to get name and will only return the distinct id therefore no repetitions
    public int getElfId(String elfName) {

        System.out.println("\nQ1 - Account for " + elfName);

        int id = -1;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "Select distinct accountId from viewed where viewerName =?;"
            );


            preparedStatement.setString(1, elfName);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                id = resultSet.getInt("accountId");
                System.out.println(elfName + " is assosiated with account(s): " + id);
            }

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
        return id;
    }


    //this method will take in the id from the getElfId method and take the name once again(for printing purposes) and give us all the bills with amounts
    public void getBills(int id, String name) {

        System.out.println("\nQ2 Bills for " + name);
        try {
            PreparedStatement preparedStatement1 = connection.prepareStatement(
                    "Select billId,amount from accounts where accountId =?;"
            );

            preparedStatement1.setInt(1, id);
            ResultSet resultSet1 = preparedStatement1.executeQuery();
            System.out.println("\nBills are:");
            while (resultSet1.next()) {
                int billId = resultSet1.getInt("billId");
                int amount = resultSet1.getInt("amount");
                System.out.println(name + " has bill " + billId + " which is for " + amount + "c");
            }

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }


    //this method will return the video info and views (from another internal method) using a query in videos
    public void getVideoInfo(String link) {


        System.out.println("\nQ3 - views for video with link " + link);

        try {
            PreparedStatement preparedStatement1 = connection.prepareStatement(
                    "Select creatorName,videoName,duration from videos where link =?;"
            );

            preparedStatement1.setString(1, link);
            ResultSet resultSet1 = preparedStatement1.executeQuery();
            while (resultSet1.next()) {
                String creatorName = resultSet1.getString("creatorName");
                String videoName = resultSet1.getString("videoName");
                int views = getViews(link, false);
                System.out.println(creatorName + "'s video " + videoName + " has views: " + views);
            }

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }


    }


    //this method will run a query in the videos table to get all links assosiated with the elfName
    public void getLinks(String elfName) {

        System.out.println("\nQ4 - videos for " + elfName + " and number of views ");
        try {

            PreparedStatement preparedStatement = connection.prepareStatement(
                    "select link from videos where creatorName=?"
            );
            preparedStatement.setString(1, elfName);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String link = resultSet.getString("link");
                System.out.print(elfName + "made video with link " + link);
                getViews(link, true);
            }


        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }


    //this method will count the number of times a link repated in the views table, the link will be the input for the sql query
    // and the bool will be an on/off switch to either print the information needed or not
    private int getViews(String link, boolean printNumViews) {

        int viewCount = 0;
        try {

            PreparedStatement preparedStatement = connection.prepareStatement(
                    "select * from viewed where link=?"
            );
            preparedStatement.setString(1, link);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                viewCount++;
            }
            if (printNumViews) {
                System.out.print(" has: " + viewCount + " views\n");
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
        return viewCount;
    }


    //this method will print results if from all the links in views if there is any link with one view
    //if the bool is creator then we are inly checking if creator was the only 1 viwer otherwise someone else
    public void oneViewer(boolean creator) {

        System.out.println("\nQ5 - views of videos with no other views than the creator: ");

        int views;
        try {

            PreparedStatement preparedStatement = connection.prepareStatement(
                    "select link,creatorName,videoName from videos"
            );

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String link = resultSet.getString("link");
                views = getViews(link, false);
                if (views == 1) {
                    String creatorName = resultSet.getString("creatorName");
                    String videoName = resultSet.getString("videoName");

                    checkViewerName(creatorName, link, videoName, creator);
                }
            }


        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }


    }


    //check viewer name will get us that partiular viewer who was the one watcher of the video using the link parameter
    //the rest of the variables are for printing purposes
    private void checkViewerName(String creatorName, String link, String videoName, boolean bool) {

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "select viewerName from viewed where link=?"
            );
            preparedStatement.setString(1, link);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String viewerName = resultSet.getString("viewerName");
                if (bool & viewerName.equals(creatorName)) {
                    System.out.println("Video " + videoName + " has no other views");
                    break;
                } else if (!bool && !viewerName.equals(creatorName)) {
                    System.out.println("Video " + videoName + " has no other viewers other than " + viewerName);
                    break;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }


    }


    //this method will get the link duration views and the product of duration*views = runtime
    public void buildRunTimeTable() {

        try {
            PreparedStatement pstmt = connection.prepareStatement(
                    "select link,duration,videoName from videos"
            );
            ResultSet resultSet = pstmt.executeQuery();
            while (resultSet.next()) {
                String link = resultSet.getString("link");
                String videoName = resultSet.getString("videoName");
                int duration = resultSet.getInt("duration");
                int numViews = getViews(link, false);
                int runTime = numViews * duration;
                insertRunTimeTable(link, videoName, numViews, duration, runTime);
            }


        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }


    //this method will create a table that will have the link duration views and the product of duration*views = runtime
    public void insertRunTimeTable(String link, String videoName, int numViews, int duration, int runTime) {

        try {

            PreparedStatement addAccount = connection.prepareStatement(
                    "insert into runTimeTable (index, link, videoName, numViews, duration, runTime)"
                            + " values (?, ?, ?, ?, ?, ?);"
            );

            addAccount.setInt(1, runIndex);
            addAccount.setString(2, link);
            addAccount.setString(3, videoName);
            addAccount.setInt(4, numViews);
            addAccount.setInt(5, duration);
            addAccount.setInt(6, runTime);


            addAccount.executeUpdate();
            runIndex++;
            addAccount.close();

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }


    //this method will run a query which will order our runtime table by the rumtime in descending order
    // and only limit the views to 5
    public void getTopFive() {

        System.out.println("\nQ6 - Users with the most minute views: ");
        try {

            PreparedStatement preparedStatement = connection.prepareStatement(
                    "select videoName,runTime from runTimeTable order by runTime DESC LIMIT 5"
            );

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                String videoName = resultSet.getString("videoName");
                int runTime = resultSet.getInt("runTime");
                System.out.println("Video " + videoName + " has total time " + (runTime) + " minutes");
            }

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }
}
