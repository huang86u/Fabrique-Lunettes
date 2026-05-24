package fr.miage.backend.usine;

import bernard_flou.Fabricateur;
import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

public class Usine {
    private final Fabricateur fabricateur;
    private final Object verrouFabricateur = new Object();
    private final Map<Fabricateur.TypeLunette, Queue<String>> filesDAttente = new ConcurrentHashMap<>();
    
    private BiConsumer<String, Fabricateur.Lunette> onLunetteFabriquee;
    private BiConsumer<String, String> onErreurFabrication;

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

    public void setOnErreurFabrication(BiConsumer<String, String> callback) {
        this.onErreurFabrication = callback;
    }

    /**
     * Produit une commande directement. L'accès au fabricateur est serialise
     * car une seule machine est partagée par les appels concurrents.
     */
    public List<Fabricateur.Lunette> produire(final Map<TypeLunette, Integer> typesLunettes) {
        if (typesLunettes == null) {
            throw new IllegalArgumentException("La commande ne peut pas être nulle");
        }

        Map<Fabricateur.TypeLunette, Integer> lignes = convertirCommande(new Commande(typesLunettes));
        List<Fabricateur.Lunette> lunettes = new ArrayList<>();

        for (Map.Entry<Fabricateur.TypeLunette, Integer> ligne : lignes.entrySet()) {
            int restant = ligne.getValue();
            while (restant > 0) {
                synchronized (verrouFabricateur) {
                    int lot = Math.min(Math.max(1, fabricateur.getCapacity()), restant);
                    Fabricateur.TypeLunette[] configuration = new Fabricateur.TypeLunette[lot];
                    Arrays.fill(configuration, ligne.getKey());
                    fabricateur.configurer(configuration);
                    for (int index = 0; index < lot; index++) {
                        lunettes.add(fabricateur.fabriquer(ligne.getKey()));
                    }
                    restant -= lot;
                }
            }
        }

        return lunettes;
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
                        List<String> commandesDuLot = new ArrayList<>();
                        int lot;
                        synchronized (verrouFabricateur) {
                            int capacite = Math.max(1, fabricateur.getCapacity());
                            lot = Math.min(capacite, file.size());
                        }

                        for (int index = 0; index < lot; index++) {
                            String orderId = file.poll();
                            if (orderId != null) {
                                commandesDuLot.add(orderId);
                            }
                        }

                        try {
                            synchronized (verrouFabricateur) {
                                Fabricateur.TypeLunette[] configuration =
                                        new Fabricateur.TypeLunette[commandesDuLot.size()];
                                Arrays.fill(configuration, type);
                                fabricateur.configurer(configuration);

                                for (String orderId : commandesDuLot) {
                                    Fabricateur.Lunette lunette = fabricateur.fabriquer(type);
                                    if (onLunetteFabriquee != null) {
                                        onLunetteFabriquee.accept(orderId, lunette);
                                    }
                                }
                            }
                        } catch (Exception exception) {
                            System.err.println("Erreur de fabrication : " + exception.getMessage());
                            for (String orderId : commandesDuLot) {
                                notifierErreur(orderId, exception);
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

    private void notifierErreur(String orderId, Exception exception) {
        if (onErreurFabrication != null) {
            onErreurFabrication.accept(orderId, exception.getMessage());
        }
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
