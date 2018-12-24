import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.flywaydb.core.Flyway;

public class Main {

    private static HttpURLConnection WebsiteConnection;
    private static String driverName = "com.mysql.cj.jdbc.Driver";

    static ObjectMapper JsonObjectMapper = new ObjectMapper();
    static Connection con = null;
    static HashMap<String, JsonNode> RockRouteNameJson = new HashMap<String, JsonNode>();


    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String LeftHalfOfState[] = {"41.102639","-83.879169"};
        String RightHalfOfState[] = {"39.460053", "-83.791317"};
        String HalfOfState[] = {"41.152291","-81.309493"};
        String FHalfOfState[] = {"39.290203", "-82.188015"};
        String FTHalfOfState[] = {"39.849139", "-82.956721"};
        String[] CoordinateArray []={LeftHalfOfState, RightHalfOfState, HalfOfState, FHalfOfState, FTHalfOfState};

        InitializeDBForGivenState(CoordinateArray, "Ohio", "200");
    }

    static void InitializeDBForGivenState(String[][] CoordinatesArray, String State, String MaxDistance) throws IOException,
            ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        for(int i=0; i<CoordinatesArray.length; i++){
            String Latitude = CoordinatesArray[i][0];
            String Longitude = CoordinatesArray[i][1];
            String EntireUrl = CreateApiRequestUrl(Latitude, Longitude, MaxDistance);

            String JsonStringData = CreateJsonStringFromGetRequest(EntireUrl);
            JsonNode MichiganRockRoutes = ReturnJsonNodeFromString(JsonStringData);
            PopulateHashMapWithJsonRoutes(MichiganRockRoutes, State);
            WriteJsonNodeToFileUsingHashmap(State);
        }
        GetDBConnection();
        PopulateDatabaseWithHashmap();
        CloseDBConnection();
        System.out.println(RockRouteNameJson.size());
        RockRouteNameJson.clear();
    }

    static void PopulateHashMapWithJsonRoutes(JsonNode RockRoutes, String State){
        for(int i=0; i<NodeSize(RockRoutes); i++){
            if(IsRouteInState(RockRoutes.get("routes").get(i), State)==true){
                String StringifiedJsonRockRouteName = RockRoutes.get("routes").get(i).get("name").toString();
                RockRouteNameJson.put(StringifiedJsonRockRouteName, RockRoutes.get("routes").get(i));
            }
        }
    }

    static void WriteJsonNodeToFileUsingHashmap(String State) throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(String.format("RockRouteFilesByState/%sRockRoutes.json", State));
        writer.print("[");
        for (Map.Entry<String,JsonNode> entry : RockRouteNameJson.entrySet()) {
            JsonNode RouteInfoJson = entry.getValue();
            String StringifiedJson = RouteInfoJson.toString()+",";
            writer.print(StringifiedJson);
        }
        writer.print("]");
        writer.close();
    }

    static void ExecuteSQLQueryToAddJsonToDB(String StringifiedJson) throws SQLException {
        String SqlInsertStatement = "INSERT INTO \"RockRouteSchema\".\"RockRouteTable\"(\"RockRouteInfo\") VALUES (cast(? as json));";
        PreparedStatement p = con.prepareStatement(SqlInsertStatement);
        p.setString(1, StringifiedJson);
        p.execute();
    }

    static void PopulateDatabaseWithHashmap() throws SQLException{
        for (Map.Entry<String,JsonNode> entry : RockRouteNameJson.entrySet()) {
            JsonNode RouteInfoJson = entry.getValue();
            String StringifiedJson = RouteInfoJson.toString()+",";
            StringifiedJson = StringifiedJson.replace("\"", "\\\"");
            StringifiedJson = String.format("\"%s\"", StringifiedJson);
            ExecuteSQLQueryToAddJsonToDB(StringifiedJson);
        }
    }

    static void RunFlyway(){
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

    static String CreateApiRequestUrl(String lat, String lon, String MaxDistance){
        String Url = "https://www.mountainproject.com/data/get-routes-for-lat-lon?";
        String UrlParameters = String.format("lat=%s&lon=%s&maxDistance=%s&minDiff=5.7&maxDiff=5.15&maxResults=500&key=200395695-227bc86ae2ef8282f4fc0b650e3c6d1f", lat, lon, MaxDistance);
        return Url + UrlParameters;
    }

    static String CreateJsonStringFromGetRequest(String RequestUrlString){
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
                if(JsonLine.contains("'")) {
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

    static void PrintNamesOfRockRoutesGivenJsonNode(JsonNode JsonNode){
        /*
        * This is more of a helper function for me, this isnt really necessary to test
        */
        for(int i = 0; i<JsonNode.get("routes").size(); i++){
            System.out.println(JsonNode.get("routes").get(i).get("name"));
        }
    }

    static void PrintJsonNodeByState(JsonNode JsonNode, String State){
        /*
         * This is more of a helper function for me, this isnt really necessary to test
         */
        for(int i = 0; i<JsonNode.get("routes").size(); i++){
            if((JsonNode.get("routes").get(i).get("location").get(0).toString().replace("\"", "")).equals(State)){
                System.out.println(JsonNode.get("routes").get(i).toString());
            }
        }
    }

    static int NodeSize(JsonNode JsonNode){
        return JsonNode.get("routes").size();
    }

    static boolean IsRouteInState(JsonNode JsonNode, String State) {
        if(JsonNode.get("location").get(0).toString().replace("\"", "").equals(State)) {
            return true;
        }
        return false;
    }
}
