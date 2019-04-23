/* Snap4City Engager (SE)
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package edu.unifi.disit.snapengager.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.unifi.disit.snapengager.datamodel.Experience;

@Service
public class ExperienceServiceImpl implements IExperienceService {

	private static final Logger logger = LogManager.getLogger();

	@Value("${experience.path}")
	private String experiencePath;

	@Override
	public List<Experience> getRandomize(String org, String group, Locale lang) throws Exception {
		String path = experiencePath + File.separator + org + File.separator + group;

		Random r = new Random(System.currentTimeMillis());
		Integer i = r.nextInt(new File(path).listFiles().length) + 1;

		return create(new File(path + File.separator + i.toString() + "_" + lang + ".txt"));

	}

	private List<Experience> create(File input) throws IOException {

		logger.debug("Randomized called on {}", input.getAbsolutePath());

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
		List<Experience> toreturn = new ArrayList<Experience>();
		String action;

		while ((action = reader.readLine()) != null) {
			String text = reader.readLine();
			Experience e = new Experience(action, text);
			toreturn.add(e);
		}

		reader.close();
		return toreturn;
	}
}