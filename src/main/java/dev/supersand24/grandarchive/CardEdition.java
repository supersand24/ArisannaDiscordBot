package dev.supersand24.grandarchive;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CardEdition {
    private String uuid;
    private String collector_number;
    private String image;

    @SerializedName("default_edition")
    private boolean defaultEdition;

    public static class CardSet {
        private String name;
        private String prefix;
        public String getName() { return name; }
        public String getPrefix() { return prefix; }
    }
    private CardSet set;

    // Flip Card Data
    private String configuration;
    private String orientation;

    @SerializedName("other_orientations")
    private List<Card> otherOrientations;

    // Injected parent links (Ignored by Gson)
    private transient String parentCardUuid;
    private transient String parentEditionUuid;

    public String getUuid() { return uuid; }
    public String getCollectorNumber() { return collector_number; }
    public boolean isDefaultEdition() { return defaultEdition; }
    public CardSet getSet() { return set; }

    // Assuming you prepend the base URL for Discord
    public String getFullImageUrl() {
        return image != null ? "https://api.gatcg.com" + image : null;
    }

    public String getConfiguration() { return configuration; }
    public String getOrientation() { return orientation; }
    public List<Card> getOtherOrientations() { return otherOrientations; }

    public String getParentCardUuid() { return parentCardUuid; }
    public void setParentCardUuid(String parentCardUuid) { this.parentCardUuid = parentCardUuid; }

    public String getParentEditionUuid() { return parentEditionUuid; }
    public void setParentEditionUuid(String parentEditionUuid) { this.parentEditionUuid = parentEditionUuid; }
}