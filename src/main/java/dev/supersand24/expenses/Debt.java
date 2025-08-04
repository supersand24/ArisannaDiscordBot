package dev.supersand24.expenses;

import dev.supersand24.CurrencyUtils;
import dev.supersand24.Identifiable;
import dev.supersand24.events.Event;
import dev.supersand24.events.EventManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class Debt implements Identifiable {

    private transient long debtId;
    private final long eventId;
    private final String debtorId;
    private final String creditorId;
    private final double amount;
    private boolean isPaid;

    public Debt(long debtId, long eventId, String debtorId, String creditorId, double amount) {
        this.debtId = debtId;
        this.eventId = eventId;
        this.debtorId = debtorId;
        this.creditorId = creditorId;
        this.amount = amount;
        this.isPaid = false;
    }

    public long getDebtId() { return debtId; }
    public long getEventId() { return eventId; }
    public String getDebtorId() { return debtorId; }
    public String getCreditorId() { return creditorId; }
    public double getAmount() { return amount; }
    public boolean isPaid() { return isPaid; }

    public void markAsPaid() {
        this.isPaid = true;
    }

    @Override
    public void setId(long id) {
        debtId = id;
    }

    public EmbedBuilder createEmbed(JDA jda) {
        User debtor = jda.retrieveUserById(debtorId).complete();
        User creditor = jda.retrieveUserById(creditorId).complete();
        Event event = EventManager.getEventById(eventId);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.RED);
        embed.setTitle("Debt Details: " + debtor.getName() + " → " + creditor.getName());

        embed.addField("Amount Owed", CurrencyUtils.formatAsUSD(amount), false);
        embed.addField("For Event", event != null ? event.getName() : "Unknown Event", false);

        List<PaymentInfo> paymentInfos = ExpenseManager.getPaymentInfoForUser(creditorId);
        if (!paymentInfos.isEmpty()) {
            StringBuilder paymentDescription = new StringBuilder();
            for (PaymentInfo info : paymentInfos) {
                paymentDescription.append(String.format("**%s:** `%s`\n", info.getAppName(), info.getDetail()));
            }
            embed.addField("How to Pay " + creditor.getName(), paymentDescription.toString(), false);
        } else {
            embed.addField("How to Pay " + creditor.getName(), "This user has not added any payment information.", false);
        }

        return embed;
    }
}
