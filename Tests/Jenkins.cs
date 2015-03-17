// Copyright (c) 2015 SIL International
// This software is licensed under the MIT License (http://opensource.org/licenses/MIT)
using System;
using OpenQA.Selenium.PhantomJS;
using OpenQA.Selenium.Support.UI;
using OpenQA.Selenium;
using System.Drawing;

namespace Tests
{
	public class Jenkins: IDisposable
	{
		public const string Url = "http://jenkins-vm.local:8080";
		private PhantomJSDriver _driver;
		private WebDriverWait _wait;

		private Jenkins()
		{
			var options = new PhantomJSOptions();
			options.AddAdditionalCapability("phantomjs.page.customHeaders.Accept-Language", "en");
			_driver = new PhantomJSDriver(options);
			_wait = new WebDriverWait(_driver, TimeSpan.FromSeconds(10));
		}

		#region Disposable stuff

		#if DEBUG
		/// <summary/>
		~Jenkins()
		{
			Dispose(false);
		}
		#endif

		/// <summary/>
		public bool IsDisposed { get; private set; }

		/// <summary/>
		public void Dispose()
		{
			Dispose(true);
			GC.SuppressFinalize(this);
		}

		/// <summary/>
		protected virtual void Dispose(bool fDisposing)
		{
			System.Diagnostics.Debug.WriteLineIf(!fDisposing, "****** Missing Dispose() call for " + GetType() + ". *******");
			if (fDisposing && !IsDisposed)
			{
				// dispose managed and unmanaged objects
				if (_driver != null)
					_driver.Dispose();
				_driver = null;
			}
			IsDisposed = true;
		}
		#endregion

		public static Jenkins Connect()
		{
			return new Jenkins();
		}

		public void Login()
		{
			_driver.Navigate().GoToUrl(Url + "/login");
			_driver.Manage().Window.Size = new Size(1024, 768);
			_wait.Until(d => d.Title.StartsWith("Jenkins"));
			var username = _driver.FindElementById("j_username");
			username.SendKeys("admin");
			var pw = _driver.FindElementByName("j_password");
			pw.SendKeys("admin");
			var loginButton = _driver.FindElementById("yui-gen1-button");
			loginButton.Click();
		}

		public void OpenPage(string page)
		{
			_driver.Url = string.Format("{0}/job/{1}/configure", Url, page);
			_wait.Until(d => d.Title.StartsWith(page));
		}

		public PhantomJSDriver Driver
		{
			get { return _driver; }
		}

		public bool IsChecked(string label)
		{
			return _driver.FindElementByXPath(
				string.Format("//input[following-sibling::label[text()=\"{0}\"]]", label)).Selected;
		}

		public bool IsCheckedByName(string inputName)
		{
			return _driver.FindElementByName(inputName).Selected;
		}

		public bool IsCheckedByXPath(string xpath)
		{
			return _driver.FindElementByXPath(xpath).Selected;
		}

		public string GetValue(string label)
		{
			return _driver.FindElementByXPath(
				string.Format("//td[preceding-sibling::td[text()=\"{0}\"]]/input", label)).GetAttribute("value");
		}

		public string GetValueByName(string inputName)
		{
			return _driver.FindElementByName(inputName).GetAttribute("value");
		}

		public string GetValueByXPath(string xpath)
		{
			return _driver.FindElementByXPath(xpath).GetAttribute("value");
		}

		public string GetValueByXPath(string parentLabel, string xpath)
		{
			var table = _driver.FindElementByXPath(string.Format(
				"//tbody[tr/td/div/b[text()='{0}']]", parentLabel));
			return table.FindElement(By.XPath(xpath)).GetAttribute("value");
		}

		public string GetTextByName(string inputName)
		{
			return _driver.FindElementByName(inputName).GetAttribute("textContent");
		}

		public string GetTextByXPath(string xpath)
		{
			return _driver.FindElementByXPath(xpath).GetAttribute("textContent");
		}

		public string GetTextByXPath(string parentLabel, string xpath)
		{
			var table = _driver.FindElementByXPath(string.Format(
				"//tbody[tr/td/div/b[text()='{0}']]", parentLabel));
			return table.FindElement(By.XPath(xpath)).GetAttribute("textContent");
		}
	}
}

