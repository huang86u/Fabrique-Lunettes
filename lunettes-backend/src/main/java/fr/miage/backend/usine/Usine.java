package fr.miage.backend.usine;

import bernard_flou.Fabricateur;
import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

public class Usine {
    private final Fabricateur fabricateur;
    private final Map<Fabricateur.TypeLunette, Queue<String>> filesDAttente = new ConcurrentHashMap<>();
    
    private BiConsumer<String, Fabricateur.Lunette> onLunetteFabriquee;

    public Usine() {
        this.fabricateur = new Fabricateur();
        // Initialisation des files d'attente
        for (Fabricateur.TypeLunette type : Fabricateur.TypeLunette.values()) {
            filesDAttente.put(type, new ConcurrentLinkedQueue<>());
        }
        demarrerProduction();
    }

    public void setOnLunetteFabriquee(BiConsumer<String, Fabricateur.Lunette> callback) {
        this.onLunetteFabriquee = callback;
    }

    // Ajoute simplement la commande à la file, sans bloquer !
    public void ajouterCommande(String orderId, Commande commande) {
        Map<Fabricateur.TypeLunette, Integer> typesLunettes = convertirCommande(commande);
        for (Map.Entry<Fabricateur.TypeLunette, Integer> ligne : typesLunettes.entrySet()) {
            for (int i = 0; i < ligne.getValue(); i++) {
                filesDAttente.get(ligne.getKey()).add(orderId);
            }
        }
    }

    private void demarrerProduction() {
        Thread thread = new Thread(() -> {
            while (true) {
                boolean aProduit = false;
                
                for (Fabricateur.TypeLunette type : Fabricateur.TypeLunette.values()) {
                    Queue<String> file = filesDAttente.get(type);
                    
                    if (!file.isEmpty()) {
                        int capacite = Math.max(1, fabricateur.getCapacity());
                        int lot = Math.min(capacite, file.size()); // On groupe jusqu'à la capacité max !
                        
                        Fabricateur.TypeLunette[] configuration = new Fabricateur.TypeLunette[lot];
                        Arrays.fill(configuration, type);
                        
                        fabricateur.configurer(configuration);
                        
                        // Fabrication du lot groupé
                        for (int index = 0; index < lot; index++) {
                            String orderId = file.poll();
                            if (orderId != null) {
                                try {
                                    Fabricateur.Lunette lunette = fabricateur.fabriquer(type);
                                    if (onLunetteFabriquee != null) {
                                        onLunetteFabriquee.accept(orderId, lunette);
                                    }
                                } catch (Exception exception) {
                                    System.err.println("Erreur de fabrication : " + exception.getMessage());
                                }
                            }
                        }
                        aProduit = true;
                    }
                }
                
                // Pause légère si aucune commande n'est en attente
                if (!aProduit) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }
        }, "Thread-Usine");
        
        thread.setDaemon(true);
        thread.start();
    }

    private Map<Fabricateur.TypeLunette, Integer> convertirCommande(Commande commande) {
        Map<Fabricateur.TypeLunette, Integer> lignes = new EnumMap<>(Fabricateur.TypeLunette.class);
        for (Map.Entry<TypeLunette, Integer> entry : commande.lignes().entrySet()) {
            try {
                Fabricateur.TypeLunette type = Fabricateur.TypeLunette.valueOf(entry.getKey().name());
                lignes.put(type, entry.getValue());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Type de lunettes non supporte : " + entry.getKey().name(), exception);
            }
        }
        return lignes;
    }
}