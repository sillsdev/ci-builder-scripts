// Copyright (c) 2015 SIL International
// This software is licensed under the MIT License (http://opensource.org/licenses/MIT)
using System;
using NUnit.Framework;
using OpenQA.Selenium;

namespace Tests
{
	[TestFixtureAttribute]
	public class BloomViewsTests
	{
		private Jenkins _jenkins;

		[TestFixtureSetUp]
		public void FixtureSetUp()
		{
			_jenkins = Jenkins.Connect();
			_jenkins.Login();
		}

		[TestFixtureTearDown]
		public void FixtureTearDown()
		{
			_jenkins.Dispose();
		}

		[Test]
		public void BloomViews()
		{
			_jenkins.OpenPage("view/Bloom/view/All", "All");
			var tabs = _jenkins.Driver.FindElementsByXPath("//div[@class='tabBar']/div/a");
			Assert.That(tabs.Count, Is.EqualTo(6));
			Assert.That(tabs[0].Text, Is.EqualTo("All"));
			Assert.That(tabs[1].Text, Is.EqualTo("Build Pipeline master branch"));
			Assert.That(tabs[2].Text, Is.EqualTo("PR pipeline Version3.3"));
			Assert.That(tabs[3].Text, Is.EqualTo("PR pipeline master branch"));
			Assert.That(tabs[4].Text, Is.EqualTo("Package builds"));
			Assert.That(tabs[5].Text, Is.EqualTo("+"));
		}

		[Test]
		public void AllView()
		{
			_jenkins.OpenConfigurePage("view/Bloom/view/All", "Edit View");
			Assert.That(_jenkins.GetValueByName("name"), Is.EqualTo("All"));
			Assert.That(_jenkins.GetTextByXPath(
				"//td[preceding-sibling::td[text()='Description']]/textarea"),
				Is.EqualTo("All <b>Bloom</b> jobs"));
			Assert.That(_jenkins.IsCheckedByName("filterQueue"), Is.False,
				"'Filter build queue' is checked");
			Assert.That(_jenkins.IsCheckedByName("filterExecutors"), Is.False,
				"'Filter build executors' is checked");
			Assert.That(_jenkins.IsCheckedByName("useincluderegex"), Is.True,
				"'Use a regular expression to include jobs into the view' is not checked");
			Assert.That(_jenkins.GetValueByName("includeRegex"),
				Is.EqualTo("(^Bloom(-|_).*|^GitHub-Bloom-.*)"));

			var groupingTable = _jenkins.Driver.FindElementsByXPath(
				"//tbody[tr/td/div/b[text()='Regex Grouping Rule']]");
			Assert.That(groupingTable.Count, Is.EqualTo(6));

			Assert.That(groupingTable[0].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Regex for categorization']]/input")).GetAttribute("value"),
				Is.EqualTo("^Bloom-.*-(default|master)-debug$"));
			Assert.That(groupingTable[0].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Naming Rule']]/input")).GetAttribute("value"),
				Is.EqualTo("master branch compile jobs"));

			Assert.That(groupingTable[1].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Regex for categorization']]/input")).GetAttribute("value"),
				Is.EqualTo("^Bloom-.*-(default|master)-.*Tests$"));
			Assert.That(groupingTable[1].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Naming Rule']]/input")).GetAttribute("value"),
				Is.EqualTo("master branch unit tests"));

			Assert.That(groupingTable[2].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Regex for categorization']]/input")).GetAttribute("value"),
				Is.EqualTo(".*(Packaging).*"));
			Assert.That(groupingTable[2].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Naming Rule']]/input")).GetAttribute("value"),
				Is.Empty);

			Assert.That(groupingTable[3].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Regex for categorization']]/input")).GetAttribute("value"),
				Is.EqualTo(".*-Trigger-.*"));
			Assert.That(groupingTable[3].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Naming Rule']]/input")).GetAttribute("value"),
				Is.EqualTo("master branch builds"));

			Assert.That(groupingTable[4].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Regex for categorization']]/input")).GetAttribute("value"),
				Is.EqualTo("^GitHub.*-master-.*"));
			Assert.That(groupingTable[4].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Naming Rule']]/input")).GetAttribute("value"),
				Is.EqualTo("Pre-merge builds of GitHub pull requests (master branch)"));

			Assert.That(groupingTable[5].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Regex for categorization']]/input")).GetAttribute("value"),
				Is.EqualTo("^GitHub.*-Version.*-.*"));
			Assert.That(groupingTable[5].FindElement(By.XPath(
				".//td[preceding-sibling::td[text()='Naming Rule']]/input")).GetAttribute("value"),
				Is.EqualTo("Pre-merge builds of GitHub pull requests (Release branch)"));
		}

		[Test]
		[TestCase("Build Pipeline master branch", "master", "Bloom-Wrapper-Trigger-debug", false)]
		[TestCase("PR pipeline Version3.3", "Version3.3", "GitHub-Bloom-Wrapper-Version3.3-debug", true)]
		[TestCase("PR pipeline master branch", "master", "GitHub-Bloom-Wrapper-master-debug", true)]
		public void BuildPipelineMasterBranchView(string viewName, string branch, string initalJob,
			bool isPreMerge)
		{
			_jenkins.OpenConfigurePage(string.Format("view/Bloom/view/{0}", viewName), "Edit View");
			Assert.That(_jenkins.GetValueByName("name"), Is.EqualTo(viewName));
			string expectedDescription = isPreMerge ?
				string.Format("Pre-merge builds of the {0} branch of <b>Bloom</b>", branch)
				: string.Format("<b>Bloom</b> builds of the {0} branch", branch);
			Assert.That(_jenkins.GetTextByXPath(
				"//td[preceding-sibling::td[text()='Description']]/textarea"),
				Is.EqualTo(expectedDescription));
			Assert.That(_jenkins.IsCheckedByName("filterQueue"), Is.False,
				"'Filter build queue' is checked");
			Assert.That(_jenkins.IsCheckedByName("filterExecutors"), Is.False,
				"'Filter build executors' is checked");

			Assert.That(_jenkins.GetValue("Build Pipeline View Title"),
				Is.EqualTo(string.Format("Builds of the `{0}` branch", branch)));
			Assert.That(_jenkins.GetTextByXPath(
				"//td[preceding-sibling::td[text()='Layout']]"),
				Is.EqualTo("Based on upstream/downstream relationship"));
			Assert.That(_jenkins.GetValueByXPath(
				"//td[preceding-sibling::td[text()='Select Initial Job']]/select"),
				Is.EqualTo(initalJob));
			Assert.That(_jenkins.GetValueByXPath(
				"//td[preceding-sibling::td[text()='No Of Displayed Builds']]/select"),
				Is.EqualTo("10"));
			Assert.That(_jenkins.GetValueByXPath("//input[@name='_.triggerOnlyLatestJob' and @checked='true']"),
				Is.EqualTo("false"), "'Restrict triggers to most recent successful builds' is checked");
			Assert.That(_jenkins.GetValueByXPath("//input[@name='_.alwaysAllowManualTrigger' and @checked='true']"),
				Is.EqualTo("true"), "'Always allow manual trigger on pipeline steps' is not checked");
			Assert.That(_jenkins.GetValueByXPath("//input[@name='_.showPipelineDefinitionHeader' and @checked='true']"),
				Is.EqualTo("false"), "'Show pipeline project headers' is checked");
			Assert.That(_jenkins.GetValueByXPath("//input[@name='_.showPipelineParametersInHeaders' and @checked='true']"),
				Is.EqualTo("true"), "'Show pipeline parameters in project headers' is not checked");
			Assert.That(_jenkins.GetValueByXPath("//input[@name='_.showPipelineParameters' and @checked='true']"),
				Is.EqualTo("false"), "'Show pipeline parameters in revision box' is not checked");
			Assert.That(_jenkins.GetValue("Refresh frequency (in seconds)"), Is.EqualTo("3"));
			Assert.That(_jenkins.GetValue("URL for custom CSS files"), Is.Empty);
			Assert.That(_jenkins.GetValueByName("_.consoleOutputLinkStyle"),
				Is.EqualTo("Lightbox"));
		}

		[Test]
		public void PackageBuildsView()
		{
			_jenkins.OpenConfigurePage("view/Bloom/view/Package builds", "Edit View");
			Assert.That(_jenkins.GetValueByName("name"), Is.EqualTo("Package builds"));
			Assert.That(_jenkins.GetTextByXPath(
				"//td[preceding-sibling::td[text()='Description']]/textarea"),
				Is.EqualTo("Package builds of <b>Bloom</b>"));
			Assert.That(_jenkins.IsCheckedByName("filterQueue"), Is.False,
				"'Filter build queue' is checked");
			Assert.That(_jenkins.IsCheckedByName("filterExecutors"), Is.False,
				"'Filter build executors' is checked");
			Assert.That(_jenkins.IsCheckedByName("useincluderegex"), Is.True,
				"'Use a regular expression to include jobs into the view' is not checked");
			Assert.That(_jenkins.GetValueByName("includeRegex"),
				Is.EqualTo("^Bloom_Packaging-.*"));
		}
	}
}

