package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import java.util.*;

/** Immutable form snapshots. No client-selected class or verification flags. */
public final class VehicleAssessment {
    private VehicleAssessment(){}
    public static String text(String value,int max)throws IOException {
        if(value==null)throw invalid();int a=0,b=value.length();while(a<b&&space(value.charAt(a)))a++;while(b>a&&space(value.charAt(b-1)))b--;
        String s=value.substring(a,b);if(s.isEmpty()||s.length()>max||s.matches("(?s).*[\\x00-\\x1F\\x7F].*"))throw invalid();return s;
    }
    private static boolean space(char c){return Character.isWhitespace(c)||Character.isSpaceChar(c)||c=='\uFEFF';}
    public static final class Parameters {
        public final String make,model,powertrain,bodyType;public final long year;
        public Parameters(String make,String model,String year,String power,String body)throws IOException {
            this.make=text(make,80);this.model=text(model,80);String n=text(year,4);if(!n.matches("[0-9]{1,4}"))throw invalid();this.year=Long.parseLong(n);if(this.year<1)throw invalid();
            if(!Arrays.asList("combustion","hybrid","battery_electric").contains(power)||!Arrays.asList("sedan","crossover","minivan","other").contains(body))throw invalid();powertrain=power;bodyType=body;
        }
        public boolean keysChanged(Context c){return !make.equals(c.page.declared.make)||!model.equals(c.page.declared.model)||!Long.valueOf(year).equals(c.page.declared.year);}
        public boolean same(Parameters p){return p!=null&&make.equals(p.make)&&model.equals(p.model)&&year==p.year&&powertrain.equals(p.powertrain)&&bodyType.equals(p.bodyType);}
        JsonObject json(){JsonObject b=new JsonObject();b.addProperty("make",make);b.addProperty("model",model);b.addProperty("modelYear",year);b.addProperty("powertrain",powertrain);b.addProperty("bodyType",bodyType);return b;}
    }
    public static final class Document {
        public final String id,kind;public final long revision;final RegistrationReview.Item item;
        Document(RegistrationReview.Item i){id=i.id;kind=i.kind;revision=i.revision;item=i;}
        JsonObject json(){JsonObject b=new JsonObject();b.addProperty("documentId",id);b.addProperty("expectedRevision",revision);return b;}
    }
    public static final class Context {
        public final String accountId,driverId,vehicleId,plate;public final long scope;public final VehicleClassification.Page page;public final List<Document> documents;
        Context(AdminAccess access,long scope,RegistrationReview review,String vehicle,VehicleClassification.Page page)throws IOException {
            RegistrationReview.Item car=review.item(vehicle);if(car==null||!car.isVehicle()||car.revision!=page.revision||!vehicle.equals(page.vehicleId)||!car.make.equals(page.declared.make)||!car.model.equals(page.declared.model))throw new AdminApiClient.Failure(409,"vehicle_revision_conflict");
            accountId=access.accountId;this.scope=scope;driverId=review.driverId;vehicleId=vehicle;plate=car.plate;this.page=page;
            List<Document> values=new ArrayList<>();for(RegistrationReview.Item i:review.items)if("document".equals(i.type)&&vehicle.equals(i.vehicleId)&&"verified".equals(i.status)&&Arrays.asList("sts_front","sts_back").contains(i.kind))values.add(new Document(i));documents=Collections.unmodifiableList(values);
        }
        public boolean contains(Document doc){return documents.contains(doc);}
    }
    public static final class Selection {
        public final Context context;public final Parameters parameters;public final boolean exception;
        public final String targetPinned,basisModel;public final VehicleClassification.Rule rule;public final List<VehicleClassification.Condition> conditions;
        Selection(Context c,Parameters p,boolean exception,String target,VehicleClassification.Rule rule,String basis,List<VehicleClassification.Condition> conditions){context=c;parameters=p;this.exception=exception;targetPinned=target;this.rule=rule;basisModel=basis;this.conditions=conditions;}
        public boolean matches(Parameters p,boolean mode,String basis){return parameters.same(p)&&exception==mode&&(!mode||basisModel.equals(basis));}
    }
    static IOException invalid(){return new IOException("Invalid vehicle review input");}
}
