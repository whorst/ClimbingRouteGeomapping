import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.lang.Class;
import java.net.URL;
import java.util.HashMap;

import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;


class MainTest {

    String JsonNodeString = "{\"routes\":[{\"id\":105748490,\"name\":\"The Bastille Crack\",\"type\":\"Trad\",\"rating\":\"5.7\",\"stars\":4.5,\"starVotes\":1593,\"pitches\":5,\"location\":[\"Michigan\",\"Boulder\",\"Eldorado Canyon SP\",\"The Bastille\",\"The Bastille - N Face\"],\"url\":\"https://www.mountainproject.com/route/105748490/the-bastille-crack\",\"imgSqSmall\":\"https://cdn-files.apstatic.com/climb/1204790_sqsmall_1494042175.jpg\",\"imgSmall\":\"https://cdn-files.apstatic.com/climb/1204790_small_1494042175.jpg\",\"imgSmallMed\":\"https://cdn-files.apstatic.com/climb/1204790_smallMed_1494042175.jpg\",\"imgMedium\":\"https://cdn-files.apstatic.com/climb/1204790_medium_1494042175.jpg\",\"longitude\":-86.001,\"latitude\":39.8954}," +
            "{\"id\":105748490,\"name\":\"The Bastille Crack\",\"type\":\"Trad\",\"rating\":\"5.7\",\"stars\":4.5,\"starVotes\":1593,\"pitches\":5,\"location\":[\"Ohio\",\"Boulder\",\"Eldorado Canyon SP\",\"The Bastille\",\"The Bastille - N Face\"],\"url\":\"https://www.mountainproject.com/route/105748490/the-bastille-crack\",\"imgSqSmall\":\"https://cdn-files.apstatic.com/climb/1204790_sqsmall_1494042175.jpg\",\"imgSmall\":\"https://cdn-files.apstatic.com/climb/1204790_small_1494042175.jpg\",\"imgSmallMed\":\"https://cdn-files.apstatic.com/climb/1204790_smallMed_1494042175.jpg\",\"imgMedium\":\"https://cdn-files.apstatic.com/climb/1204790_medium_1494042175.jpg\",\"longitude\":-86.001,\"latitude\":39.8954}]}";
    ObjectMapper JsonObjectMapper = new ObjectMapper();
    JsonNode ActualJsonObject = JsonObjectMapper.readTree(JsonNodeString);
    JsonNode RouteOne = ActualJsonObject.get("routes").get(0);
    JsonNode RouteTwo = ActualJsonObject.get("routes").get(1);


    MainTest() throws IOException {
    }

    @Test
    void initializeDBForGivenState() {
    }

    @Test
    @DisplayName("Given a JsonNode and string state, assert that the state and json node are added to" +
            "the hashmap as a key values pair")
    void populateHashMapWithJsonRoutes() {
        //This is a little wonky. I had to create a copy of the hashmap in main.
        // I should probably change this later.
        Main.PopulateHashMapWithJsonRoutes(ActualJsonObject, "Michigan");
        HashMap<String, JsonNode>  CopyOfHashmap = new HashMap<String, JsonNode>( Main.RockRouteNameJson);
        Main.RockRouteNameJson.clear();
        Assertions.assertEquals(CopyOfHashmap.get("\"The Bastille Crack\""), RouteOne);
    }

    @Test
    void executeSQLQueryToAddJsonToDB() {
    }

    @Test
    void populateDatabaseWithHashmap() {
    }

    @Test
    void runFlyway() {
    }

    @Test
    void getDBConnection() {
    }

    @Test
    void closeDBConnection() {
    }

    @Test
    @DisplayName("Assert that the returned URL is the same as the expected url")
    void createApiRequest() {
        String ReturnedUrl = Main.CreateApiRequestUrl("500","500","500");
        String ActualUrl = "https://www.mountainproject.com/data/get-routes-for-lat-lon?lat=500" +
                "&lon=500&maxDistance=500&minDiff=5.7&maxDiff=5.15&maxResults=500" +
                "&key=200395695-227bc86ae2ef8282f4fc0b650e3c6d1f";
        Assertions.assertEquals(ReturnedUrl, ActualUrl);
    }

    @Test
    void createJsonStringFromGetRequest(){
    }

//    @Test
//    void readJsonLinesInFromGetRequest() throws IOException {
//        URL url = mock(URL.class);
//        HttpURLConnection MockedUrlConnection = mock(HttpURLConnection.class);
//        when(url.openConnection()).thenReturn(MockedUrlConnection);
//        //I need to figure this one out
//    }

    @Test
    @DisplayName("Given A String, Assert a json object is returned.")
    void returnJsonNodeFromString() throws IOException {
        Assertions.assertEquals(Main.ReturnJsonNodeFromString(JsonNodeString).getClass().toString(),
                "class com.fasterxml.jackson.databind.node.ObjectNode");
    }

    @Test
    @DisplayName("Given a Json Node, Assert that the proper size of the node is returned")
    void nodeSize() throws IOException {
        Assertions.assertEquals(Main.NodeSize(ActualJsonObject), 2);
    }

    @Test
    @DisplayName("Given a Json Node and String, Assert that the function returns true if the node's location" +
            "matches the state. Assert False if it doesn't")
    void isRouteInState() {
        Assertions.assertEquals(Main.IsRouteInState(RouteOne, "Michigan"), true);
        Assertions.assertEquals(Main.IsRouteInState(RouteTwo, "Michigan"), false);

    }
}