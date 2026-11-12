package dev.supersand24.grandarchive;

import com.google.gson.annotations.SerializedName;

public class CardEdition {

    private String uuid;
    private String slug;
    private String illustrator;
    private String image;
    private int rarity;

    @SerializedName("is_default") private boolean isDefault;
    @SerializedName("collector_number")  private String collectorNumber;

    private CardSet set;


    // --- Getters ---
    public String getUuid() { return uuid; }
    public String getSlug() { return slug; }
    public String getIllustrator() { return illustrator; }
    public int getRarity() { return rarity; }
    public boolean isDefault() { return isDefault; }
    public String getCollectorNumber() { return collectorNumber; }
    public CardSet getSet() { return set; }


    public String getFullImageUrl() {
        if (image == null || image.isEmpty()) return null;
        return "https://api.gatcg.com" + image;
    }

}
