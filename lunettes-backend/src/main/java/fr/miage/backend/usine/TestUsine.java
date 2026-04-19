package fr.miage.backend.usine;

import bernard_flou.Fabricateur.TypeLunette;
import bernard_flou.Fabricateur.Lunette;

import java.util.*;

public class TestUsine {

    public static void main(String[] args) {

        Usine usine = new Usine();

        // créer une commande
        Map<TypeLunette, Integer> commande = new HashMap<>();
        commande.put(TypeLunette.CLAUDE, 2);
        commande.put(TypeLunette.BANANA, 4);
        commande.put(TypeLunette.CLAUDE, 2);

        System.out.println("Lancement production");

        List<Lunette> resultat = usine.produire(commande);

        System.out.println("Résultat :");

        for (Lunette l : resultat) {
            System.out.println(l.type + " - " + l.serial);
        }
    }
}