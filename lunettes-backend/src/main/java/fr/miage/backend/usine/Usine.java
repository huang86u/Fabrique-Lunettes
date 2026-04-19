package fr.miage.backend.usine;
import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.TypeLunette;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;

public class Usine {

    private Fabricateur fabricateur;

    public Usine() {
        this.fabricateur = new Fabricateur();
    }

    public List<Fabricateur.Lunette> produire(Map<TypeLunette, Integer> typesLunettes) {

        List<Fabricateur.Lunette> resultat = new ArrayList<>();

        for (Map.Entry<TypeLunette, Integer> entry : typesLunettes.entrySet()) {

            TypeLunette type = entry.getKey();
            int quantite = entry.getValue();

            while (quantite > 0) {

                int capacity = fabricateur.getCapacity();
                int batch = Math.min(capacity, quantite);

                // CONFIGURATION
                TypeLunette[] config = new TypeLunette[batch];
                Arrays.fill(config, type);

                fabricateur.configurer(config);

                //FABRICATION
                for (int i = 0; i < batch; i++) {
                    try {
                        Fabricateur.Lunette lunette = fabricateur.fabriquer(type);
                        resultat.add(lunette);
                    } catch (Exception e) {
                        throw new RuntimeException("Erreur fabrication : " + e.getMessage());
                    }
                }

                quantite -= batch;
            }
        }

        return resultat;
    }
}