package com.github.manolo8.darkbot.backpage.dispatch;

public class Retriever {
    protected String id, name, type, tier, costString;
    protected Cost cost;
    public static class Cost{
        int credits;
        int uri;
        int permit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getCost() {
        return costString;
    }

    public void setCost(String costString) {
        this.costString = costString;
    }
    public void setCreditCost(int creditCost){
        cost = cost == null ? new Cost() : cost;
        cost.credits = creditCost;
    }
    public void setUridiumCost(int uridiumCost){
        cost = cost == null ? new Cost() : cost;
        cost.uri = uridiumCost;
    }
    public void setPermitCost(int permitCost){
        cost = cost == null ? new Cost() : cost;
        cost.permit = permitCost;
    }
    public int getCreditCost(){
        return cost.credits;
    }
    public int getUridiumCost(){
        return cost.uri;
    }
    public int getPermitCost(){
        return cost.permit;
    }
    @Override
    public String toString() {
        return "Retriever{" +
                "id=" + id +
                "name=" + name +
                "type=" + type +
                "tier=" + tier +
                "cost=" + cost +
                "}";
    }
}
