package me.rafaskb.ticketmaster.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.rafaskb.ticketmaster.TicketMaster;
import me.rafaskb.ticketmaster.models.Ticket;
import me.rafaskb.ticketmaster.sql.Controller;
import me.rafaskb.ticketmaster.utils.Lang;
import me.rafaskb.ticketmaster.utils.LangMacro;
import me.rafaskb.ticketmaster.utils.Perm;
import me.rafaskb.ticketmaster.utils.Utils;

public class CommandNew extends Command {
	private static final int INDEX_MESSAGE_START = 1;
	public List<Player> players = new ArrayList<>();
	
	public CommandNew() {
		super(Perm.USER_CREATE);
	}
	
	@Override
	protected void run(CommandSender sender, String[] args) {
		// Not a player
		if(!(sender instanceof Player)) {
			Lang.sendErrorMessage(sender, Lang.CANNOT_RUN_FROM_CONSOLE);
			return;
		}
		final Player player = ((Player) sender);
		
		// If not enough arguments
		if(args.length < 2) {
			Lang.sendErrorMessage(sender, Lang.NEW_COMMAND_USAGE);
			return;
		}
		
		// Check cooldown
		if(players.contains(player)) {
			Lang.sendErrorMessage(sender, Lang.NEW_COMMAND_COOLDOWN);
			return;
		}
		
		// Create ticket
		String request = Utils.convertArgumentsToString(args, INDEX_MESSAGE_START);
		Ticket ticket = new Ticket(player, request);
		
		// Insert ticket to database
		boolean success = Controller.insertOrUpdateTicket(ticket);
		
		// If it worked, send messages
		if(success) {
			// Add cooldown
			players.add(player);
			
			// Tell submitter
			String msgToSender = LangMacro.replaceId(Lang.NEW_COMMAND_SUCCESS, ticket.getId());
			Lang.sendMessage(sender, msgToSender);
			
			// Broadcast new ticket to online helpers
			broadcastTicketCreation(ticket);
			
			//Start cooldown reduction
			new BukkitRunnable() {
				@Override
				public void run() {
					if (players.contains(player)) {
						players.remove(player);
					}
				}
			}.runTaskLater(TicketMaster.getInstance(), 20 * 15);
		}
		
		// If it failed (should never happen), send failure message
		else {
			Lang.sendErrorMessage(sender, Lang.NEW_COMMAND_FAILURE);
		}
		
	}
	
	private void broadcastTicketCreation(Ticket ticket) {
		String perm = ticket.getPriority().getRequiredPermission();
		
		String msgToHelper = LangMacro.replaceId(Lang.NEW_TICKET_BROADCAST, ticket.getId());
		msgToHelper = LangMacro.replaceSubmitter(msgToHelper, ticket.getSubmitter());
		
		for(Player p : Utils.getOnlinePlayers())
			if(p.hasPermission(perm))
				Lang.sendMessage(p, msgToHelper);
	}
	
}
