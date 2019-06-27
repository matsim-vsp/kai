/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.greedo.recipes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.LogDataWrapper;
import org.matsim.core.gbl.MatsimRandom;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Ameli2017Recipe implements ReplannerIdentifierRecipe {

	@Override
	public String getDeployedRecipeName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public void update(LogDataWrapper logDataWrapper) {
	}

	@Override
	public boolean isReplanner(final Id<Person> personId, final double deltaScoreIfYes, final double deltaScoreIfNo,
			final double currentUtility, final double anticipatedUtilityChange) {
		final double proba = Math.max(0, Math.min(1, anticipatedUtilityChange / currentUtility));
		return (MatsimRandom.getRandom().nextDouble() < proba);
	}

}
