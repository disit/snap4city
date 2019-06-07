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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ExperienceServiceImpl implements IExperienceService {

	private static final Logger logger = LogManager.getLogger();

	@Value("${experience.path}")
	private String experiencePath;

	@Override
	public String getRandomize(String org, String group, Locale lang) throws Exception {
		String path = experiencePath + File.separator + org + File.separator + group;

		Random r = new Random(System.currentTimeMillis());

		String[] st = new File(path).list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(".*" + lang.toString() + ".*\\.txt");
			}
		});

		Integer i = r.nextInt(st.length) + 1;

		return create(new File(path + File.separator + i.toString() + "_" + lang + ".txt"));

	}

	private String create(File input) throws IOException {

		logger.debug("Randomized called on {}", input.getAbsolutePath());

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(input)));
		StringBuffer toreturn = new StringBuffer();
		String line;

		while ((line = reader.readLine()) != null) {
			toreturn.append(line);
		}

		reader.close();
		return toreturn.toString();
	}
}