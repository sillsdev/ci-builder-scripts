// Copyright (c) 2015 SIL International
// This software is licensed under the MIT License (http://opensource.org/licenses/MIT)
using System;
using NUnit.Framework;

namespace Tests
{
	[TestFixtureAttribute]
	public class GitHubBloomLinuxTestPreMergeJobTests
	{
		private Jenkins _jenkins;

		[TestFixtureSetUp]
		public void FixtureSetUp()
		{
			_jenkins = Jenkins.Connect();
			_jenkins.Login();
			_jenkins.OpenPage("GitHub-Bloom-Linux-any-master-debug-Tests");
		}

		[TestFixtureTearDown]
		public void FixtureTearDown()
		{
			_jenkins.Dispose();
		}

		[Test]
		public void StartXvfb()
		{
			Assert.That(_jenkins.IsChecked(
				"Start Xvfb before the build, and shut it down after."), Is.True,
				"'Start Xvfb before the build, and shut it down after.' not checked");
		}

		[Test]
		public void RunOnSameNode()
		{
			Assert.That(_jenkins.IsChecked(
				"Run this job on the same node where another job has last ran"), Is.True,
				"'Run this job on the same node where another job has last ran' not checked");
			Assert.That(_jenkins.GetValueByName("_.job"),
				Is.EqualTo("GitHub-Bloom-Linux-any-master-debug"));
			Assert.That(_jenkins.IsCheckedByName("_.shareWorkspace"), Is.True,
				"'Share Workspace' not checked");
		}

		[Test]
		public void NUnitPublisher()
		{
			Assert.That(_jenkins.GetValueByName("_.testResultsPattern"),
				Is.EqualTo("output/Debug/BloomTests.dll.results.xml"));
		}
	}
}

