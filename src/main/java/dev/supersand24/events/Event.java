package dev.supersand24.events;

import dev.supersand24.Identifiable;

public class Event implements Identifiable {

    private transient long eventId;
    private String name;
    private long startDate;
    private long endDate;
    private long roleId;
    private long channelId;
    private String address;
    private String omnidexLink;

    public Event(String name) {
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

}
