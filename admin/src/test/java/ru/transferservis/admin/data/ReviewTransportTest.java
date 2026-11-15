package ru.transferservis.admin.data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class ReviewTransportTest {
    private Response response(Request request,int status,String type,byte[] body){return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(status).message("fixture").body(ResponseBody.create(MediaType.get(type),body)).build();}
    @Test public void mutationsCarryRevisionAndStableIdempotencyOnlyOnFixedBackend() throws Exception {
        RegistrationReview review=ModerationTestData.review();ReviewAction action=ReviewAction.decide(ModerationTestData.access("[\"drivers.verify_documents\"]"),review,review.items.get(0),"changes_required","Synthetic reason");
        List<String> bodies=new ArrayList<>(),keys=new ArrayList<>();
        AdminApiClient api=new AdminApiClient(chain->{Request request=chain.request();assertEquals("/v1/admin/registrations/staff_driver123/review/items/document/document_fixture001",request.url().encodedPath());assertEquals("Bearer fixture-token",request.header("Authorization"));assertEquals("no-store",request.header("Cache-Control"));
            okio.Buffer buffer=new okio.Buffer();request.body().writeTo(buffer);bodies.add(buffer.readUtf8());keys.add(request.header("Idempotency-Key"));return response(request,200,"application/json","{\"ok\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8));});
        api.reviewAction("fixture-token",action);api.reviewAction("fixture-token",action);assertEquals(bodies.get(0),bodies.get(1));assertEquals(keys.get(0),keys.get(1));assertFalse(bodies.get(0).contains("accountId"));
    }
    @Test public void unauthorizedMutationIsNotReplayedAndRawErrorIsNotExposed() throws Exception {
        AtomicInteger calls=new AtomicInteger();RegistrationReview review=ModerationTestData.review();
        ReviewAction action=ReviewAction.decide(ModerationTestData.access("[\"drivers.verify_documents\"]"),review,review.items.get(0),"rejected","Synthetic reason");
        AdminApiClient api=new AdminApiClient(chain->{calls.incrementAndGet();return response(chain.request(),401,"application/json","{\"code\":\"authorization_required\",\"error\":\"private upstream text\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));});
        try{api.reviewAction("token",action);fail();}catch(AdminApiClient.Failure denied){assertEquals(401,denied.status);assertEquals("authorization_required",denied.code);assertFalse(denied.toString().contains("private upstream"));}assertEquals(1,calls.get());
    }
    @Test public void resourceMimeSizeAndPathGuardsRejectUnsafeData() throws Exception {
        RegistrationReview.Item item=ModerationTestData.review().items.get(0);
        for(String type:new String[]{"text/html","application/pdf","image/svg+xml"}){
            AdminApiClient api=new AdminApiClient(chain->response(chain.request(),200,type,new byte[]{1}));
            try{api.resource("token","staff_driver123",item);fail();}catch(IOException expected){}
        }
        AdminApiClient large=new AdminApiClient(chain->response(chain.request(),200,"image/png",new byte[10_485_761]));
        try{large.resource("token","staff_driver123",item);fail();}catch(IOException expected){}
        AtomicInteger calls=new AtomicInteger();AdminApiClient guard=new AdminApiClient(chain->{calls.incrementAndGet();throw new IOException("Unexpected transport");});
        for(String driver:new String[]{"../other","staff_a%2fother","https://evil.invalid"})try{guard.review("token",driver);fail();}catch(IOException expected){}assertEquals(0,calls.get());
    }
}
