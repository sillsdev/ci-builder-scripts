/*
 * Copyright (c) 2017 SIL International
 * File copied from https://github.com/sheehan/job-dsl-gradle-example
 * This file is licensed under the Apache License 2.0 (http://www.apache.org/licenses/)
 */
package utilities

import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.MemoryJobManagement

class JobSpecMixin {

	JobParent createJobParent() {
		JobParent jp = new JobParent() {
			@Override
			Object run() {
				return null
			}
		}
		JobManagement jm = new MemoryJobManagement()
		jp.setJm(jm)
		jp
	}
}
