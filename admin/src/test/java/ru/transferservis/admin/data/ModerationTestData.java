package ru.transferservis.admin.data;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
final class ModerationTestData {
    static JsonObject body(){return JsonParser.parseString("{\"ok\":true,\"registration\":{\"profile\":{\"driverId\":\"staff_driver123\",\"fullName\":\"Synthetic Driver\",\"region\":\"Test\",\"revision\":2,\"registrationStatus\":\"under_review\",\"identityStatus\":\"pending\"},\"vehicles\":[],\"documents\":[{\"id\":\"document_fixture001\",\"kind\":\"passport_main\",\"status\":\"pending\",\"revision\":1}],\"photos\":[],\"reviews\":[]}}").getAsJsonObject();}
    static RegistrationReview review() throws Exception{return RegistrationReview.parse(body(),"staff_driver123");}
    static AdminAccess access(String permissions){return AdminAccess.fromResponse(JsonParser.parseString("{\"ok\":true,\"admin\":{\"accountId\":\"admin-1\",\"role\":\"dispatcher\",\"permissions\":"+permissions+"}}").getAsJsonObject(),"admin-1");}
}
