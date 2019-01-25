import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.postgresql.util.PGobject;
import org.flywaydb.core.Flyway;


public class Main {

    private static HttpURLConnection WebsiteConnection;
    private static String driverName = "com.mysql.cj.jdbc.Driver";

    static ObjectMapper JsonObjectMapper = new ObjectMapper();
    static Connection con = null;
    static HashMap<String, JsonNode> RockRouteNameJson = new HashMap<String, JsonNode>();


    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {

//        IterateOverStatesForPartitionedTables();
//        InitializeDBGivenJsonFile();

//        GetDBConnection();
//        ExecuteSQLQueryToReturnJsonFromDB();
//        CloseDBConnection();

    }

    static void IterateOverStatesForPartitionedTables() throws IOException,
            ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        JsonNode StateCoordinates = CreateJsonStateLatLonObjFromFile("C:\\Users\\William\\Projects\\MountainProjectApiTest\\staticfiles\\StateLatLonJson.json");
        int i = 0;
        for (JsonNode node : StateCoordinates) {
            String StateName = node.get("name").toString().replace("\"", "");
            GetDBConnection();
            CreatePartitionedTableGivenState(StateName);
            CloseDBConnection();
        }
    }

    static void InitializeDBGivenJsonFile() throws IOException,
            ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

        JsonNode StateCoordinates = CreateJsonStateLatLonObjFromFile("C:\\Users\\William\\Projects\\MountainProjectApiTest\\staticfiles\\StateLatLonJson.json");
        for (JsonNode node : StateCoordinates) {
            IterateOverStateLatitudeAndLongitude(node);

        }
    }

    static void IterateOverStateLatitudeAndLongitude(JsonNode StateNode) throws IOException, ClassNotFoundException,
            SQLException, InstantiationException, IllegalAccessException {
        double MinLatitude = StateNode.get("min_lat").asDouble();
        double MinLongitude = StateNode.get("min_lng").asDouble();
        double MaxLatitude = StateNode.get("max_lat").asDouble();
        double MaxLongitude = StateNode.get("max_lng").asDouble();

        double CurrentLatitude = MinLatitude;
        double CurrentLongitude = MinLongitude;
        double PreviousLatitude = MinLatitude;
        double PreviousLongitude = MinLongitude;

        String CurrentState = StateNode.get("name").toString().replace("\"", "");

        while (CurrentLongitude < MaxLongitude) {
            while (CurrentLatitude < MaxLatitude) {

                double MilesBetweenCurrentAndPreviousPoints = DistanceBetweenLatitudeAndLongitude(PreviousLatitude, PreviousLongitude, CurrentLatitude, CurrentLongitude, "M");
                InitializeDBForGivenStateTwo(CurrentState, CurrentLatitude, CurrentLongitude, MilesBetweenCurrentAndPreviousPoints);
                PreviousLatitude = CurrentLatitude;
                CurrentLatitude++;
            }
            CurrentLongitude++;
            PreviousLongitude = CurrentLongitude;
            CurrentLatitude = MinLatitude + 1;
            PreviousLatitude = MinLatitude;
        }
        GetDBConnection();
        PopulateDatabaseWithHashmap(CurrentState);
        CloseDBConnection();
        RockRouteNameJson.clear();
    }


    static void InitializeDBForGivenStateTwo(String State, Double Latitude, Double Longitude, Double Distance) throws IOException{

        if (Distance == 0) {
            Distance = 50.0;
        }
        int IntegerDistance = (int) Math.ceil(Distance);
        String EntireUrl = CreateApiRequestUrl(Latitude.toString(), Longitude.toString(), Integer.toString(IntegerDistance));
        String JsonStringData = CreateJsonStringFromGetRequest(EntireUrl);
        JsonNode RockRoutes = ReturnJsonNodeFromString(JsonStringData);
        PopulateHashMapWithJsonRoutes(RockRoutes, State);
    }

    public static double DistanceBetweenLatitudeAndLongitude(double LatitudeOne, double LongitudeOne, double LatitudeTwo,
                                                             double LongitudeTwo, String unit) {
        double theta = LongitudeOne - LongitudeTwo;
        double Distance = Math.sin(Math.toRadians(LatitudeOne)) * Math.sin(Math.toRadians(LatitudeTwo)) + Math.cos(Math.toRadians(LatitudeOne)) * Math.cos(Math.toRadians(LatitudeTwo)) * Math.cos(Math.toRadians(theta));
        Distance = Math.acos(Distance);
        Distance = Math.toDegrees(Distance);
        Distance = Distance * 60 * 1.1515;
        if (unit == "K") {
            Distance = Distance * 1.609344;
        } else if (unit == "N") {
            Distance = Distance * 0.8684;
        }
        return (Distance);
    }

    static void PopulateHashMapWithJsonRoutes(JsonNode RockRoutes, String State) {
        for (int i = 0; i < NodeSize(RockRoutes); i++) {
            if (IsRouteInState(RockRoutes.get("routes").get(i), State) == true) {
                String StringifiedJsonRockRouteId = RockRoutes.get("routes").get(i).get("id").toString();
                RockRouteNameJson.put(StringifiedJsonRockRouteId, RockRoutes.get("routes").get(i));
            }
        }
    }

    static void ExecuteSQLQueryToReturnJsonFromDB() throws SQLException, IOException {
        String SqlReturnStatement = "SELECT * FROM \"RockRouteSchema\".\"RockRouteInfoTest\" WHERE \"RockRouteInfo\"->>'id' = '105843919' ;";
        PreparedStatement p = con.prepareStatement(SqlReturnStatement);
        ResultSet rs = p.executeQuery();
        while (rs.next()) {
            System.out.println(rs.getString(1));
        }
    }

    static void ExecuteSQLQueryToAddJsonToDB(String StringifiedJson, String CurrentState) throws SQLException, IOException {

        String SqlInsertStatement = String.format("INSERT INTO \"RockRouteSchema\".\"RockRouteTable%s\"(\"RockRouteInfo\") VALUES (cast(? as JSONB));", CurrentState);
        ;
        PreparedStatement p = con.prepareStatement(SqlInsertStatement);
        p.setString(1, StringifiedJson);
        p.execute();
    }


    static void CreatePartitionedTableGivenState(String State) throws SQLException {
        String createStatement = String.format("CREATE TABLE \"RockRouteSchema\".\"RockRouteTable%s\" (CONSTRAINT \"RockRouteInfo_%s_Check\" CHECK ((\"RockRouteInfo\" -> 'location')->> 0 = '%s'::text)) INHERITS (\"RockRouteSchema\".\"RockRouteInfoTest\");", State, State, State);
        System.out.println(createStatement);
        PreparedStatement p = con.prepareStatement(createStatement);
        p.execute();
    }

    static void PopulateDatabaseWithHashmap(String CurrentState) throws SQLException, IOException {
//        System.out.println(RockRouteNameJson.size());
        //PrintHashMap(RockRouteNameJson);
        for (Map.Entry<String, JsonNode> entry : RockRouteNameJson.entrySet()) {
            JsonNode RouteInfoJson = entry.getValue();
            String StringifiedJson = RouteInfoJson.toString();
//            PrintHashMap(RockRouteNameJson);
//            System.out.println(StringifiedJson);
//            StringifiedJson = StringifiedJson.replace("\"", "\\\"");
//            StringifiedJson = StringifiedJson.substring(0, StringifiedJson.length() - 1);
//            StringifiedJson = String.format("\"%s\"", StringifiedJson);
            ExecuteSQLQueryToAddJsonToDB(StringifiedJson, CurrentState);
        }
    }

    static void RunFlyway() {
        Flyway flyway = new Flyway();
        flyway.setDataSource("jdbc:postgresql://localhost:5432/RockRouteDatabase", "admin", "Bedfordbroncos13");
        flyway.migrate();
    }

    static void GetDBConnection() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String url = "jdbc:postgresql://localhost:5432/RockRouteDatabase";
        String user = "admin";
        String password = "Bedfordbroncos13";
        Class.forName(driverName);
        con = DriverManager.getConnection(url, user, password);
    }

    static void CloseDBConnection() throws SQLException {
        con.close();
    }

    static String CreateApiRequestUrl(String lat, String lon, String MaxDistance) {
        String Url = "https://www.mountainproject.com/data/get-routes-for-lat-lon?";
        String UrlParameters = String.format("lat=%s&lon=%s&maxDistance=%s&minDiff=5.7&maxDiff=5.15&maxResults=500&key=200395695-227bc86ae2ef8282f4fc0b650e3c6d1f", lat, lon, MaxDistance);
        return Url + UrlParameters;
    }

    static JsonNode CreateJsonStateLatLonObjFromFile(String FilePath) throws IOException {
        String entireJson = ReadJsonLinesInFromFile(FilePath);
        return ReturnJsonNodeFromString(entireJson);
    }

    static String ReadJsonLinesInFromFile(String FilePath) throws IOException {
        StringBuilder content = new StringBuilder("");
        String JsonLine;

        File file = new File(FilePath);

        BufferedReader br = new BufferedReader(new FileReader(file));
        while ((JsonLine = br.readLine()) != null) {
            content.append(JsonLine);
        }
        br.close();
        return content.toString();
    }


    static String CreateJsonStringFromGetRequest(String RequestUrlString) {
        String entireJson = null;
        try {
            //Set Website Connection Given URL and Parameters
            URL MtProjUrl = new URL(RequestUrlString);
            WebsiteConnection = (HttpURLConnection) MtProjUrl.openConnection();
            WebsiteConnection.setRequestMethod("GET");

            entireJson = ReadJsonLinesInFromGetRequest(WebsiteConnection);

        } finally {
            WebsiteConnection.disconnect();
            return entireJson;
        }
    }

    static String ReadJsonLinesInFromGetRequest(HttpURLConnection WebsiteConnection) throws IOException {
        StringBuilder content = null;
        String JsonLine;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(WebsiteConnection.getInputStream()))) {
            content = new StringBuilder();
            while ((JsonLine = in.readLine()) != null) {
                if (JsonLine.contains("'")) {
                    JsonLine = JsonLine.replace("'", "");
                }
                content.append(JsonLine);
                content.append(System.lineSeparator());
            }
        }
        return content.toString();
    }

    static JsonNode ReturnJsonNodeFromString(String JsonString) throws IOException {
        return JsonObjectMapper.readTree(JsonString);
    }


    static int NodeSize(JsonNode JsonNode) {
        return JsonNode.get("routes").size();
    }

    static boolean IsRouteInState(JsonNode JsonNode, String State) {
//        System.out.println(JsonNode.get("location").get(0).toString().replace("\"", "").equals(State));
        if (JsonNode.get("location").get(0).toString().replace("\"", "").equals(State)) {
            return true;
        }
        return false;
    }

    static void WriteJsonNodeToFileUsingHashmap(String State) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(String.format("RockRouteFilesByState/%sRockRoutes.json", State));
        writer.print("[");
        for (Map.Entry<String, JsonNode> entry : RockRouteNameJson.entrySet()) {
            JsonNode RouteInfoJson = entry.getValue();
            String StringifiedJson = RouteInfoJson.toString() + ",";
            writer.print(StringifiedJson);
        }
        writer.print("]");
        writer.close();
    }

    static void PrintNamesOfRockRoutesGivenJsonNode(JsonNode JsonNode) {
        /*
         * This is more of a helper function for me, this isnt really necessary to test
         */
        for (int i = 0; i < JsonNode.get("routes").size(); i++) {
            System.out.println(JsonNode.get("routes").get(i).get("name"));
        }
    }

    public static void PrintHashMap(HashMap mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
//            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    static void PrintJsonNodeByState(JsonNode JsonNode, String State) {
        /*
         * This is more of a helper function for me, this isnt really necessary to test
         */
        for (int i = 0; i < JsonNode.get("routes").size(); i++) {
            if ((JsonNode.get("routes").get(i).get("location").get(0).toString().replace("\"", "")).equals(State)) {
                System.out.println(JsonNode.get("routes").get(i).toString());
            }
        }
    }

    static PGobject ReturnPostgresObjectFromString(String StringifiedJson) throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(StringifiedJson);
        return jsonObject;
    }

    static void StupidIdiot(String CurrentState) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        GetDBConnection();
        String StringifiedJson = "{\"id\": 105748490,\"name\": \"The Bastille Crack\",\"location\": [\"Michigan\",\"Boulder\",\"Eldorado Canyon SP\",\"The Bastille\",\"The Bastille - N Face\"]}";
        System.out.println(StringifiedJson);
        String SqlInsertStatement = String.format("INSERT INTO \"RockRouteSchema\".\"RockRouteTable%s\"(\"RockRouteInfo\") VALUES (cast(? as JSONB));", CurrentState);
        System.out.println(SqlInsertStatement);
        PreparedStatement p = con.prepareStatement(SqlInsertStatement);
        p.setString(1, StringifiedJson);
        p.execute();
        CloseDBConnection();
    }

    //    static void InitializeDBForGivenState(String[][] CoordinatesArray, String State, String MaxDistance) throws IOException,
//            ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
//        for(int i=0; i<CoordinatesArray.length; i++){
//            String Latitude = CoordinatesArray[i][0];
//            String Longitude = CoordinatesArray[i][1];
//            String EntireUrl = CreateApiRequestUrl(Latitude, Longitude, MaxDistance);
//
//            String JsonStringData = CreateJsonStringFromGetRequest(EntireUrl);
//            JsonNode MichiganRockRoutes = ReturnJsonNodeFromString(JsonStringData);
//            PopulateHashMapWithJsonRoutes(MichiganRockRoutes, State);
//            WriteJsonNodeToFileUsingHashmap(State);
//        }
//        GetDBConnection();
//        PopulateDatabaseWithHashmap();
//        CloseDBConnection();
//        System.out.println(RockRouteNameJson.size());
//        RockRouteNameJson.clear();
//    }
}
