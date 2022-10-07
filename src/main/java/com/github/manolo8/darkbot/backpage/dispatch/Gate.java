package com.github.manolo8.darkbot.backpage.dispatch;

public class Gate {
    protected String id, name, time, cost, collectable;

    protected boolean isAvailable, inProgress;

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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getCollectable() {
        return collectable;
    }

    public void setCollectable(String collectable) {
        this.collectable = collectable;
    }

    public boolean getInProgress(){ return inProgress; }

    public void setInProgress(boolean inProgress){ this.inProgress = inProgress; }

    public boolean getIsAvailable(){ return isAvailable; }

    public void setIsAvailable(boolean isAvailable){ this.isAvailable = isAvailable; }

    @Override
    public String toString() {
        return "Retriever{" +
                "id=" + id +
                "name=" + name +
                "time=" + time +
                "cost=" + cost +
                "collectable=" + collectable +
                "isAvailable" + isAvailable +
                "inProgress=" + inProgress +
                "}";
    }
}
