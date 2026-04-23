package dev.supersand24.events;

import dev.supersand24.IData;
import dev.supersand24.groups.GroupData;

import java.util.ArrayList;
import java.util.List;

public class EventData implements IData {

    private transient long eventId;
    private String name = "";
    private long startDate = 0;
    private long endDate = 0;
    private long roleId = 0;
    private long channelId = 0;
    private String address = "";
    private String omnidexLink = "";
    private String ticketLink = "";
    private boolean gaugeInterest = false;
    private List<Long> interestedMembers = new ArrayList<>();
    private final List<GroupData> groups = new ArrayList<>();

    public EventData(String name) {
        this.name = name;
    }

    public long getId() { return eventId; }
    @Override public void setId(long id) { eventId = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public long getRoleId() { return roleId; }
    public void setRoleId(long roleId) { this.roleId = roleId; }
    public long getChannelId() { return channelId; }
    public void setChannelId(long channelId) { this.channelId = channelId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getOmnidexLink() { return omnidexLink; }
    public void setOmnidexLink(String omnidexLink) { this.omnidexLink = omnidexLink; }
    public String getTicketLink() { return ticketLink; }
    public void setTicketLink(String ticketLink) { this.ticketLink = ticketLink; }
    public boolean isGaugeInterest() { return gaugeInterest; }
    public void setGaugeInterest(boolean gaugeInterest) { this.gaugeInterest = gaugeInterest; }
    public List<Long> getInterestedMembers() {
        if (interestedMembers == null) {
            interestedMembers = new ArrayList<>();
        }
        return interestedMembers;
    }
    public void addInterestedMember(long memberId) { getInterestedMembers().add(memberId); }
    public void removeInterestedMember(long memberId) { getInterestedMembers().remove(memberId); }

}
