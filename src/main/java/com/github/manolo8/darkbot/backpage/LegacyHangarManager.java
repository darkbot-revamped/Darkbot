package com.github.manolo8.darkbot.backpage;

import com.github.manolo8.darkbot.Main;
import com.github.manolo8.darkbot.backpage.entities.*;
import com.github.manolo8.darkbot.utils.Base64Utils;
import com.github.manolo8.darkbot.utils.http.Method;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Deprecated
public class LegacyHangarManager {

    private final Gson GSON;
    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final Type DRONE_LIST = new TypeToken<List<Drone>>(){}.getType();
    private static final Type ITEMINFO_LIST = new TypeToken<List<ItemInfo>>(){}.getType();
    private static final Type ITEM_LIST = new TypeToken<List<Item>>(){}.getType();
    private static final Type SHIPINFO_LIST = new TypeToken<List<ShipInfo>>(){}.getType();
    private static final Type STRING_LIST = new TypeToken<List<String>>(){}.getType();

    private final Main main;
    private final BackpageManager backpageManager;
    private long lastHangarChange = 0;
    private List<Hangar> hangars;
    private List<Drone> drones;
    private List<Item> items;
    private List<ItemInfo> itemInfos;
    private List<ShipInfo> shipInfos;
    private List<String> types;
    private List<String> lootIds;
    private long lastHangarDataUpdate = 0;

    public LegacyHangarManager(Main main, BackpageManager backpageManager) {
        this.main = main;
        this.backpageManager = backpageManager;
        this.hangars = new ArrayList<>();
        this.drones = new ArrayList<>();
        this.items = new ArrayList<>();
        this.itemInfos = new ArrayList<>();
        this.shipInfos = new ArrayList<>();
        this.GSON = backpageManager.getGson();
    }

    public String getDataInventory(String params) {
        try {
            return backpageManager.getConnection(params, Method.GET, 2500)
                    .setRawHeader("Content-Type", "application/x-www-form-urlencoded")
                    .consumeInputStream(Base64Utils::decode);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean changeHangar(String hangarId) {
        if (lastHangarChange > System.currentTimeMillis() || !backpageManager.sidStatus().contains("OK")) return false;
        try {
            JsonObject paramObj = new JsonObject();
            JsonObject hangarObj = new JsonObject();

            hangarObj.addProperty("hi", getActiveHangar());
            hangarObj.addProperty("hangarId", hangarId);

            paramObj.add("params", hangarObj);
            
            return backpageManager.getConnection("flashAPI/inventory.php", Method.POST, 2000)
                    .addSupplier(() -> this.lastHangarChange = System.currentTimeMillis() + 12_000)
                    .setRawHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setRawParam("action", "activateShip")
                    .setParam("params", Base64Utils.encode(paramObj.toString()))
                    .consumeInputStream(Base64Utils::decode)
                    .contains("\"isError\":0");
        } catch (IOException e) {
            e.printStackTrace();
            this.lastHangarChange = System.currentTimeMillis() + 5_000;
            return false;
        }
    }

    public Boolean checkDrones() {
        updateHangarData(500);
        boolean repaired = !drones.isEmpty();
        for (Drone drone : drones) {
            if (drone.getDamage() / 100d >= main.config.MISCELLANEOUS.DRONE_REPAIR_PERCENTAGE) {
                repaired &= repairDrone(drone);
            }
        }
        return repaired;
    }

    private boolean repairDrone(Drone drone) {
        try {
            String encodeParams = Base64Utils.encode( "{\"action\":\"repairDrone\",\"lootId\":\""
                    + drone.getLoot() + "\",\"repairPrice\":" + drone.getRepairPrice() +
                    ",\"params\":{\"hi\":" + getActiveHangar() + "}," +
                    "\"itemId\":\"" + drone.getItemId() + "\",\"repairCurrency\":\"" + drone.getRepairCurrency() +
                    "\",\"quantity\":1,\"droneLevel\":" + drone.getDroneLevel() + "}");
            String url = "flashAPI/inventory.php?action=repairDrone&params="+encodeParams;
            String json = this.backpageManager.getDataInventory(url);
            return json.contains("'isError':0");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateHangars() {
        String params = "flashAPI/inventory.php?action=getHangarList";
        String json = backpageManager.getDataInventory(params);

        if (json == null || !(json.contains("'isError':0") || json.contains("\"isError\":0"))) return;

        JsonObject ret = JSON_PARSER.parse(json).getAsJsonObject().get("data").getAsJsonObject().get("ret").getAsJsonObject();
        hangars.clear();
        forEachHangar(ret, h -> hangars.add(GSON.fromJson(h, Hangar.class)));
    }

    public String getActiveHangar() {
        return hangars.stream().filter(Hangar::hangarIsActive).findFirst().map(Hangar::getHangarId).orElse(null);
    }

    public void updateHangarData(int expiryTime) {
        if (System.currentTimeMillis() > lastHangarDataUpdate + expiryTime) updateHangarData();
    }

    private void updateHangarData() {
        updateHangars();
        String hangarId = getActiveHangar();
        if (hangarId == null) return;

        String encodeParams = Base64Utils.encode("{\"params\":{\"hi\":" + hangarId + "}}");
        String json = this.backpageManager.getDataInventory("flashAPI/inventory.php?action=getHangar&params=" + encodeParams);

        if (json != null) {
            JsonObject data = JSON_PARSER.parse(json).getAsJsonObject().getAsJsonObject("data");
            JsonObject ret = data.getAsJsonObject("ret");
            forEachHangar(ret, h -> {
                if (!h.get("hangar_is_active").getAsBoolean()) return;
                this.drones = GSON.fromJson(h.getAsJsonObject("general").get("drones"), DRONE_LIST);
            });
            this.items = GSON.fromJson(ret.get("items"), ITEM_LIST);
            this.itemInfos = GSON.fromJson(ret.get("itemInfo"), ITEMINFO_LIST);
            this.shipInfos = GSON.fromJson(ret.get("shipInfo"), SHIPINFO_LIST);
            this.types = GSON.fromJson(data.getAsJsonObject("map").get("types"), STRING_LIST);
            this.lootIds = GSON.fromJson(data.getAsJsonObject("map").get("lootIds"), STRING_LIST);
            lastHangarDataUpdate = System.currentTimeMillis();
        }
    }

    private void forEachHangar(JsonObject ret, Consumer<JsonObject> consumer) {
        try {
            JsonElement val = ret.get("hangars");
            if (val instanceof JsonArray) val.getAsJsonArray().forEach(i -> consumer.accept(i.getAsJsonObject()));
            else val.getAsJsonObject().entrySet().forEach(i -> consumer.accept(i.getValue().getAsJsonObject()));
        } catch (Exception e) {
            System.err.println("Failed to iterate hangars: " + ret);
            throw e;
        }
    }

    public List<Drone> getDrones() {
        return drones;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<ItemInfo> getItemInfos() {
        return itemInfos;
    }

    public List<ShipInfo> getShipInfos() {
        return shipInfos;
    }

    public List<String> getTypes() {
        return types;
    }

    public List<String> getLootIds() {
        return lootIds;
    }
}
