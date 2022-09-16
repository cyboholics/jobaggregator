package tech.portfolioshop.jobs.models.kafka;

import org.json.JSONObject;

public class UserDeleted extends Payload{
    private String userId;

    public UserDeleted(String userId) {
        super("USER_DELETED");

        this.userId = userId;
    }

    public String serialize(){
        JSONObject obj = new JSONObject();
        obj.put("userId", userId);
        return obj.toString();
    }
    public void deserialize(String json){
        JSONObject obj = new JSONObject(json);
        userId = obj.getString("userId");
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}