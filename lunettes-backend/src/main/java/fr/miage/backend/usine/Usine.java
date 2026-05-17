package fr.miage.backend.usine;

import bernard_flou.Fabricateur;
import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Usine {
    private final Fabricateur fabricateur;

    public Usine() {
        this.fabricateur = new Fabricateur();
    }

    public List<Fabricateur.Lunette> produire(Commande commande) {
        return produire(convertirCommande(commande));
    }

    public synchronized List<Fabricateur.Lunette> produire(final Map<Fabricateur.TypeLunette, Integer> typesLunettes) {
        List<Fabricateur.Lunette> resultat = new ArrayList<>();

        for (Map.Entry<Fabricateur.TypeLunette, Integer> ligneCommande : typesLunettes.entrySet()) {
            Fabricateur.TypeLunette type = ligneCommande.getKey();
            int quantite = ligneCommande.getValue();

            while (quantite > 0) {
                int capacite = Math.max(1, fabricateur.getCapacity());
                int lot = Math.min(capacite, quantite);

                Fabricateur.TypeLunette[] configuration = new Fabricateur.TypeLunette[lot];
                Arrays.fill(configuration, type);
                fabricateur.configurer(configuration);

                for (int index = 0; index < lot; index++) {
                    try {
                        resultat.add(fabricateur.fabriquer(type));
                    } catch (Exception exception) {
                        throw new IllegalStateException("Fabrication impossible", exception);
                    }
                }

                quantite -= lot;
            }
        }

        return resultat;
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
