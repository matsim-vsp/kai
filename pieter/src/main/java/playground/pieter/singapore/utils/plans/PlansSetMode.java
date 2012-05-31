package playground.pieter.singapore.utils.plans;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

public class PlansSetMode {

	public void run(Population plans) {
		System.out.println("    running " + this.getClass().getName()
				+ " algorithm...");

		Iterator<Id> pid_it = plans.getPersons().keySet().iterator();
		int countCarPlans=0;
		while (pid_it.hasNext()) {
			Id personId = pid_it.next();
			PersonImpl person = (PersonImpl) plans.getPersons().get(personId);

			for (int i = person.getPlans().size() - 1; i >= 0; i--) {
				Plan plan = person.getPlans().get(i);
				boolean carDriver = false;
				for (int j = 1; j < plan.getPlanElements().size(); j += 2) {
					LegImpl leg = (LegImpl) plan.getPlanElements().get(j);

					if (j==1 && leg.getMode().equals("car")) {
						carDriver = true;
						countCarPlans++;
					}

					if (carDriver)
						leg.setMode("car");;
				}
			}

		}

		// okay, now remove in a 2nd step all persons we do no longer need
		System.out.println("Fixed "+ countCarPlans + " plans to have car mode only.");
		System.out.println("processed "+ plans.getPersons().size()+" plans");
		System.out.println("    done.");

	}

}
