package dev.supersand24.cardStore;

import dev.supersand24.IData;

import java.util.ArrayList;
import java.util.List;

public class CardStoreData implements IData {

    private transient long storeId;
    private String name = "";
    private String address = "";
    private String website = "";
    private List<Long> localPlayers = new ArrayList<>();

    public long getId() { return storeId; }
    @Override public void setId(long id) { storeId = id; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public CardStoreData() {

    }

}
