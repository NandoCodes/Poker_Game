package Main;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class PlayPanel extends JPanel {
	private CircularArrayList<Player> allowedPlayers;// circular arrayList

	private TablePot tablePot;
	private Dealer dealer;
	private boolean betRound;// will be used to check which betting round im in

	public PlayPanel() {
		setUpTable();
	}

	public void setUpTable() {
		dealer = new Dealer();
		tablePot = new TablePot();
		allowedPlayers = dealer.determinePlayers(PokerGame.getPlayers());
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		if (allowedPlayers.size() > 1 && allowedPlayers.size() < 7) {

			JLabel allowed = new JLabel("<html>Only players with more money than 200 money are allowed to play<br/>"
					+ "The following players are allowed to play:");
			allowed.setAlignmentX(Component.CENTER_ALIGNMENT);
			add(allowed);

			for (Player p : allowedPlayers) {
				JLabel text = new JLabel(p.getName());
				text.setAlignmentX(Component.CENTER_ALIGNMENT);
				add(text);
			}

			JButton continueButton = new JButton("Start");
			continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
			continueButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					removeAll();
					ante();
					validate();
					repaint();
				}
			});
			add(continueButton);
			returnMain();

		} else if (allowedPlayers.size() < 2) {
			JLabel warning = new JLabel("You need at least 2 players with enough money to play");
			add(warning);
			returnMain();

		}
	}

	// The ante gets money from each player before they get their cards.
	public void ante() {
		int anteSum=dealer.collectAnte(allowedPlayers);
		tablePot.addToMainPot(anteSum);// collect antes and add the amount to the main pot
		
		allowedPlayers.setUpButton();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		JLabel anteMess = new JLabel(
				"An ante of " + dealer.getAnte() + " has been take from each player and added to the pot");
		add(anteMess, gbc);
		JButton continueButton = new JButton("Continue");
		add(continueButton, gbc);
		anteMess.setAlignmentX(Component.CENTER_ALIGNMENT);
		continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		continueButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeAll();
				collectBlinds();
				validate();
				repaint();
			}
		});
	}

	public void collectBlinds() {
		Player one = allowedPlayers.getBlindPlayer(1);
		Player two = allowedPlayers.getBlindPlayer(2);
		dealer.collectBlinds(one, two, tablePot);
		//Lets say Player B has 15 coins left, he pays an ante of 10 and is chosen for the blind of 20. What happens is
		//he is forced to go all in with 5 coins, so the current maxBid is set to the max of player B or the smallBlind player.
		tablePot.increaseBid(Math.max(tablePot.getCurrentBet(one),tablePot.getCurrentBet(two)));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		JLabel sBlindMess = new JLabel(
				"A small blind of " + tablePot.getCurrentBet(one) + " has been taken from " + one.getName());
		JLabel bBlindMess = new JLabel(
				"A big blind of " + tablePot.getCurrentBet(two) + " has been taken from " + two.getName());
		add(sBlindMess, gbc);
		add(bBlindMess, gbc);
		JButton continueButton = new JButton("Continue");
		add(continueButton, gbc);
		sBlindMess.setAlignmentX(Component.CENTER_ALIGNMENT);
		bBlindMess.setAlignmentX(Component.CENTER_ALIGNMENT);
		continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		continueButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeAll();
				dealer.dealCards(allowedPlayers);
				playerTurn(allowedPlayers.getNext());
				validate();
				repaint();
			}
		});
	}

	// Asks players one by one which cards they want to switch
	public void switchCards(Player player) {
		removeAll();
		if (player != null&&player.isFolded()!=true) {
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			JLabel mess = new JLabel("It is now the switch cards phase.");
			JLabel message = new JLabel("It is " + player.getName() + "'s turn!");
			JLabel message2 = new JLabel("You have the following cards. Choose to switch or skip.");
			JButton next = new JButton("Next");
			add(mess, gbc);
			add(message, gbc);
			add(message2, gbc);

			// Make a JPanel for each card.
			for (Card card : player.getHand()) {
				JPanel hand = new JPanel();
				JLabel display = new JLabel(card.toString());
				JButton switchButton = new JButton("Switch");
				switchButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						JButton button = (JButton) e.getSource();
						JPanel panel = (JPanel) button.getParent();
						player.removeCard(card);
						remove(panel);
						validate();
						repaint();
					}
				});
				hand.add(display);
				hand.add(switchButton);
				add(hand, gbc);
			}
			next.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					switchCards(allowedPlayers.getNext());
					validate();
					repaint();
				}
			});
			add(next);

		} else {
			allowedPlayers.reset();
			dealer.dealCards(allowedPlayers);
			playerTurn(allowedPlayers.getNext());
		}
	}

	// The call/raise/fold turn.
	public void playerTurn(Player player) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		if (player != null&&player.isFolded()!=true&&player.isAllIn()!=true) {
			removeAll();
			JLabel mess = new JLabel("Your cards have been dealt!");
			add(mess, gbc);
			JLabel turn = new JLabel("It is " + player.getName() + " turn");
			add(turn, gbc);
			JLabel pot = new JLabel(
					"The current MaxBet is of " + tablePot.getMaxBid() + " and the pot is " + tablePot.getMainPot());
			JLabel cards = new JLabel("These are your cards:");
			JPanel hand = displayHand(player);
			add(pot, gbc);
			add(cards, gbc);
			add(hand, gbc);
			JLabel sideMess=new JLabel("Your current bet is of: "+ tablePot.getCurrentBet(player)); 
			JLabel message = new JLabel("Your current money is:" + player.getMoney());
			JPanel buttons = new JPanel();
            add(sideMess,gbc);
			add(message, gbc);
			if (tablePot.getMaxBid() > tablePot.getCurrentBet(player)) {
				JLabel call2 = new JLabel(
						"Would you like to call: " + (tablePot.getMaxBid() - tablePot.getCurrentBet(player)));
				JButton call = new JButton("Call");
				call.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						call();
					}
				});
				buttons.add(call);
				add(call2, gbc);
			} else {
				JButton check = new JButton("Check");
				check.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						playerTurn(allowedPlayers.getNext());
						validate();
						repaint();
					}
				});
				buttons.add(check);
			}
			JButton raise = new JButton("Raise");
			raise.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					raise();
				}
			});
			buttons.add(raise);
			JButton fold = new JButton("Fold");
			fold.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					fold();
				}
			});
			buttons.add(fold);
			add(buttons, gbc);
		} else if (!betRound) {
			betRound = true;
			tablePot.addToMainPot(tablePot.getBetsTotal());
			allowedPlayers.reset();
			switchCards(allowedPlayers.getNext());
		} else {
			tablePot.addToMainPot(tablePot.getBetsTotal());
			allowedPlayers.reset();
			displayWinner();
			return;
		}

	}

	public void displayWinner() {
		removeAll();
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		Player pWinner = decideWinner();
		JLabel done = new JLabel("These are everyone's cards: ");
		add(done, gbc);
		for (int x = 0; x < allowedPlayers.size(); x++) {
			JPanel display = new JPanel();
			JLabel name = new JLabel(allowedPlayers.get(x).getName() + " ");
			JPanel hand = displayHand(allowedPlayers.get(x));
			display.add(name);
			display.add(hand);
			add(display, gbc);
		}
		JLabel winner = new JLabel(pWinner.getName() + " won with " + pWinner.getHandRank().getRanking());
		JLabel added = new JLabel(tablePot.getMainPot() + " has been added to " + pWinner.getName());
		pWinner.addMoney(tablePot.distributeMainPot());
		add(winner, gbc);
		add(added, gbc);
		resetGame();
		returnMain();

	}

	public JPanel displayHand(Player player) {
		JPanel hand = new JPanel();
		for (Card card : player.getHand()) {
			JLabel cards = new JLabel(card.toString());
			hand.add(cards);
		}
		return hand;
	}



	public void defaultWin() {
		removeAll();
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		JLabel winner = new JLabel("Since everyone else folded the winner is: " + allowedPlayers.get(0).getName());
		tablePot.addToMainPot(tablePot.getBetsTotal());
		JLabel winner2 = new JLabel(allowedPlayers.get(0).getName() + " has won " + tablePot.getMainPot() + " money");

		allowedPlayers.get(0).addMoney(tablePot.distributeMainPot());

		winner.setAlignmentX(Component.CENTER_ALIGNMENT);
		winner2.setAlignmentX(Component.CENTER_ALIGNMENT);

		add(winner);
		add(winner2);
		returnMain();
		resetGame();
		validate();
		repaint();
		return;
	}

	public Player decideWinner() {
		for (Player player : allowedPlayers) {
			Ranking.evaluateHand(player);
		}
		Collections.sort(allowedPlayers, new Comparator<Player>() {
			public int compare(Player one, Player two) {
				for (HandRanking a : one.getHandValue().keySet()) {
					for (HandRanking b : two.getHandValue().keySet()) {
						int result = a.ordinal() - b.ordinal();
						if (result != 0)
							return result;
						return one.getHandValue().get(a) - two.getHandValue().get(b);
					}
				}
				return 0;

			}
		});
		return allowedPlayers.get(allowedPlayers.size() - 1);
	}

	private void returnMain() {
		JButton returnButton = new JButton("Main Menu");
		returnButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JButton button = (JButton) e.getSource();
				JPanel panel = (JPanel) button.getParent();
				JFrame frame = (JFrame) SwingUtilities.getRoot(panel);
				frame.remove(panel);
				frame.add(new MenuPane());
				frame.validate();
				frame.repaint();
			}
		});
		returnButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(returnButton);
	}

	// resets all variables when a round is over
	public void resetGame() {
		tablePot = new TablePot();
		Deck.discardDeck();
		betRound = false;
		for (Player player : allowedPlayers) {
			player.clearHand();
			allowedPlayers.reset();
		}
		allowedPlayers.clear();
	}

	public void raise() {
		removeAll();
		JLabel message = new JLabel("Enter amount to raise");
		JTextField amount = new JTextField(10);
		JButton enter = new JButton("Enter");
		add(message);
		add(amount);
		add(enter);
		enter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					removeAll();
					int amount2 = Integer.parseInt(amount.getText());
					if (allowedPlayers.getCurrPlayer().getMoney() > amount2) {
						allowedPlayers.getCurrPlayer().substractMoney(amount2);
						tablePot.increaseBid(amount2);
						JLabel mess = new JLabel("Amount betting has been raised to" + tablePot.getMaxBid());
						add(mess);
						allowedPlayers.setEnd();
						tablePot.setCurrentBet(allowedPlayers.getCurrPlayer(), amount2);
						playerTurn(allowedPlayers.getNext());
						validate();
						repaint();

					} else {
						JLabel warning = new JLabel("Cant raise more than what you have. Make another choice");
						add(warning);
						raise();
						validate();
						repaint();
					}

				} catch (NumberFormatException ex) {
					JLabel warning = new JLabel("Please dont use words");
					add(warning);
					raise();
					validate();
					repaint();
				}

			}
		});
		validate();
		repaint();
	}

	public void fold() {
		removeAll();
		JLabel message = new JLabel(allowedPlayers.getCurrPlayer().getName() + " has folded");
		add(message);
			allowedPlayers.getCurrPlayer().setFold();
			playerTurn(allowedPlayers.getNext());
			validate();
			repaint();
	}

	public void call() {
		removeAll();
		int difference = tablePot.getMaxBid() - tablePot.getCurrentBet(allowedPlayers.getCurrPlayer());

		if (allowedPlayers.getCurrPlayer().getMoney() >= difference) {
			tablePot.setCurrentBet(allowedPlayers.getCurrPlayer(), difference);
			allowedPlayers.getCurrPlayer().substractMoney(difference);
			JLabel message = new JLabel(allowedPlayers.getCurrPlayer().getName() + " has called");
			add(message);
			playerTurn(allowedPlayers.getNext());
			validate();
			repaint();
		} else {
			JLabel message = new JLabel("You do not have enough money. Choose another option");
			add(message);
			call();
			validate();
			repaint();
		}
	}
}
