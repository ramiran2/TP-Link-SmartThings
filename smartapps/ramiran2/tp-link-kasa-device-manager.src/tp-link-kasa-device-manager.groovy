/*
TP-Link Connect Service Manager, 2018 Version 2

Copyright 2018 Dave Gutheinz

Licensed under the Apache License, Version 2.0 (the "License"); you 
may not use this file except in compliance with the License. You may 
obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0
		
Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
implied. See the License for the specific language governing 
permissions and limitations under the License.

TP-Link Kasa Device Manager, 2018 Version 2

Copyright 2018 Anthony Ramirez

Licensed under the Apache License, Version 2.0 (the "License"); you 
may not use this file except in compliance with the License. You may 
obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0
		
Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
implied. See the License for the specific language governing 
permissions and limitations under the License.

##### Discalimer: This Service Manager and the associated Device 
Handlers are in no way sanctioned or supported by TP-Link. All
development is based upon open-source data on the TP-Link Kasa Devices; 
primarily various users on GitHub.com.

##### Notes #####
1.	This Service Manager is designed to install and manage TP-Link 
	bulbs, plugs, and switches using their respective device handlers.
2.	Please direct comments to the SmartThings community thread 
	'Cloud TP-Link Device SmartThings Integration'.

##### History #####
2018-09-28 Improved UI Elements with other small changes + Added a login page + Updated Driver Version Variables + Added a New Device Handler
2018-09-27 Improved UI Elements with other small changes + Updated for new Device Handlers + Add Support for the new Smart Thing Application
2018-08-28 Improved UI Elements with other small changes
2018-08-27 Improved UI Elements with other large changes
2018-08-22 Improved UI Elements and updated the app logo plus other small changes
2018-08-11 Updated for Added Support for update from a repo on smartthings website + Improved app name + Added app version
2018-01-31 Updated for new release of Device Handlers
*/

definition(
	name: "TP-Link Kasa Device Manager",
	namespace: "ramiran2",
	author: "Dave Gutheinz (Modified by xKillerMaverick)",
	description: "A Service Manager for the TP-Link Kasa Devices connecting through the TP-Link Servers",
	category: "SmartThings Labs",
	iconUrl: "${getAppImg("kasa_logo.png")}",
	iconX2Url: "${getAppImg("kasa_logo.png")}",
	iconX3Url: "${getAppImg("kasa_logo.png")}",
	singleInstance: true
	)
	
	def appVersion() { "2.4.0" }
	def appVerDate() { "09-28-2018" }
	def appAuthor() { "Dave Gutheinz (Modified by xKillerMaverick)" }
	def driverVersionsMin() {
		return [
			"colorbulbenergymonitor":["val":230, "desc":"2.3.0"],
			"colorbulb":["val":230, "desc":"2.3.0"],
			"dimmingswitch":["val":230, "desc":"2.3.0"],
			"energymonitorplug":["val":230, "desc":"2.3.0"],
			"plug":["val":230, "desc":"2.3.0"],
			"switch":["val":230, "desc":"2.3.0"],
			"softwhitebulbenergymonitor":["val":230, "desc":"2.3.0"],
			"softwhitebulb":["val":230, "desc":"2.3.0"],
			"tunablewhitebulbenergymonitor":["val":230, "desc":"2.3.0"],
			"tunablewhitebulb":["val":230, "desc":"2.3.0"]
		]
	}

preferences {
	page(name: "startPage")
	page(name: "authPage")
	page(name: "mainPage")
	page(name: "selectDevices")
	page(name: "devMode")
}

def setInitialStates() {
	if (!state.TpLinkToken) {state.TpLinkToken = null}
	if (!state.devices) {state.devices = [:]}
	if (!state.currentError) {state.currentError = null}
	if (!state.errorCount) {state.errorCount = 0}
}

//This Page is used to load either parent or child app interface code
def startPage() {
	atomicState?.isParent = true
	setInitialStates()
	if ("${userPassword}" == "" || "${userPassword}" == "" ){
		authPage()
	} else {
		mainPage()
	}
}

//	----- LOGIN PAGE -----
def authPage() {
	def authPageText = "If possible, open the IDE and select Live Logging. Then, " +
		"enter your Username and Password for TP-Link (same as Kasa app) and the "+
		"action you want to complete. " + "Your current token:\n\r" + "${state.TpLinkToken}" +
		"\n\rAvailable actions:\n\r" +
		"Activate Account: Login into TP-Link Account and obtains token and adds devices.\n\r" +
		"Update Account: Updates the token."
		def hideInfoDiagDescCont = (true)
		def hideInfoDiagDescStat = (state.TpLinkToken != null)
	return dynamicPage(
		name: "authPage",
		title: "TP-Link Kasa - Login Page",
		nextPage: "selectDevices",
		install: (atomicState?.isInstalled == true ? true : false),
		uninstall: false) {
		section("") {
			paragraph appSmallInfoDesc(), image: getAppImg("kasa_logo.png")
		}
		section("Information Description:", hideable: hideInfoDiagDescCont, hidden: hideInfoDiagDescStat) {
			paragraph title: "Information:", authPageText
		}
		section("Login Page:") {
			input(
				"userName", "email",
				title: "TP-Link Kasa Email Address",
				required: true,
				displayDuringSetup: true,
				image: getAppImg("email.png")
			)
			input(
				"userPassword", "password",
				title: "TP-Link Kasa Account Password",
				required: true,
				displayDuringSetup: true,
				image: getAppImg("password.png")
			)
		}
		section("Configuration Page:") {
			input(
				"userSelectedOption", "enum",
				title: "What do you want to do?",
				required: true,
				multiple: false,
				options: ["Activate Account", "Update Account"],
				image: getAppImg("settings.png")
			)
			input(
				"userSelectedDevMode", "bool",
				title: "Do you want to enable developer mode?",
				image: getAppImg("developer.png")
			)
		}
	}
}

//	----- SETTINGS PAGE -----
def mainPage() {
	def mainPageText = "Your current token:\n\r" + "${state.TpLinkToken}" +
		"\n\rAvailable actions:\n\r" +
		"Initial Install: Login into TP-Link Account and obtains token and adds devices.\n\r" +
		"Add Devices: Only add devices.\n\r" +
		"Update Token: Updates the token." +
		"Communication Error: Disables your capability to go the next page untill you fix the issue at hand."
	def errorRetuInfo = "We will not be unable to load TP-Link Kasa - Device Settings Page until you fix any error that show up in diagnostics.\n" + "Attempting to override this will end up in a blank screen."
	def hideInfoDiagDescCont = (true)
	def hideInfoDiagDescStat = (state.currentError == null)
	def errorMsg = ""
	getDevices()
	def devices = state.devices
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
	}
	if (state.currentError != null){
		errorMsg = "Error communicating with cloud:\n\r" + "${state.currentError}" +
			"\n\rPlease resolve the error and try again."
		}
	return dynamicPage(
		name: "mainPage", 
		title: "TP-Link Kasa - Settings Page", 
		nextPage: "selectDevices", 
		uninstall: false) {
		section("") {
			paragraph appInfoDesc(), image: getAppImg("kasa_logo.png")
		}
        section("Diagnostics/Information Description:", hideable: hideInfoDiagDescCont, hidden: hideInfoDiagDescStat) {
			if (state.currentError != null || state.currentError == "none"){
				paragraph title: "Communication Error:", errorMsg
			}
			if (userSelectedOption == "Communication Error"){
				paragraph title: "Loading Error:", errorRetuInfo
			}
			if (state.currentError == null || state.currentError == "none"){
				paragraph title: "Information:", mainPageText
			}
		}
		section("Configuration Page:") {
			if (state.currentError != null && isChild != null || state.currentError == "none" && isChild != null) {
				input(
					"userSelectedOption", "enum",
					title: "What do you want to do?",
					required: true,
					multiple: false,
					options: ["Communication Error", "Developer Page"],
					image: getAppImg("error.png")
				)
			} else {
				input(
					"userSelectedOption", "enum",
					title: "What do you want to do?",
					required: true,
					multiple: false,
					options: ["Initial Install", "Add Devices", "Update Token"],
					image: getAppImg("settings.png")
				)
			input(
				"userSelectedDevMode", "bool",
				title: "Do you want to enable developer mode?",
				image: getAppImg("developer.png")
			)
		}
	}
}

//	----- SELECT DEVICES PAGE -----
def selectDevices() {
	if (userSelectedOption != "Activate Account" && userSelectedOption != "Add Devices" && userSelectedOption != "Update Token" && userSelectedOption != "Update Account" && userSelectedOption != "Communication Error") {
		return authPage()
	}
	if (userSelectedOption != "Add Devices" && userSelectedOption != "Update Token" && userSelectedOption != "Update Account" && userSelectedOption != "Activate Account") {
		return mainPage()
	}
	if (userSelectedOption == "Update Token" || userSelectedOption == "Activate Account" || userSelectedOption == "Update Account") {
		getToken()
	}
	getDevices()
	def devices = state.devices
	def errorMsg = ""
	def newDevices = [:]
	devices.each {
		def isChild = getChildDevice(it.value.deviceMac)
		if (!isChild) {
			newDevices["${it.value.deviceMac}"] = "${it.value.alias} model ${it.value.deviceModel}"
		}
	}
	settings.selectedDevices = null
	if (devices == [:]) {
		errorMsg = "We were unable to find any TP-Link Kasa devices on your account. This usually means "+
		"that all devices are in 'Local Control Only'. Correct them then " +
		"rerun the application."
		}
	if (newDevices == [:]) {
		errorMsg = "No new devices to add. Are you sure they are in Remote " +
		"Control Mode?"
		}
	def hideInfoDiagDescCont = (true)
	def hideInfoDiagDescStat = (errorMsg == "")
	def TPLinkDevicesMsg = "TP-Link Token is ${state.TpLinkToken}\n\r" +
		"Devices that have not been previously installed and are not in 'Local " +
		"WiFi control only' will appear below. Tap below to see the list of " +
		"TP-Link Kasa Devices available select the ones you want to connect to " +
		"SmartThings.\n\r" + "Press Done when you have selected the devices you " +
		"wish to add, thenpress Done again to install the devices. Press	<	" +
		"to return to the previous page."
	return dynamicPage(
		name: "selectDevices", 
		title: "TP-Link Kasa - Device Setup Page", 
		install: true,
		uninstall: true) {
		section("") {
			paragraph appSmallInfoDesc(), image: getAppImg("kasa_logo.png")
		}
			section("Diagnostics/Information Description:", hideable: hideInfoDiagDescCont, hidden: hideInfoDiagDescStat) {
					if (userSelectedOption != "Update Token" && userSelectedOption != "Update Account" && errorMsg != "") {
						paragraph title: "Device Error:", errorMsg
					}
					if (errorMsg == ""){
						paragraph title: "Information:", TPLinkDevicesMsg
					}
			}
		if (userSelectedOption == "Update Token" || userSelectedOption == "Update Account") {
			section("Account Configuration Page:") {
				input(
					"userSelected", "enum",
					title: "What do you want to do?",
					required: true,
					multiple: false,
					options: ["Update Token"],
					image: getAppImg("token.png")
					)
				}
			}
		if (userSelectedOption == "Add Devices" || userSelectedOption == "Activate Account") {
			section("Device Configuration Page:") {
				input(
					"selectedDevices", "enum",
					required: true,
					multiple: true,
					title: "Select Devices (${newDevices.size() ?: 0} found)",
					options: newDevices,
					image: getAppImg("devices.png")
				)
			}
		}
	}
}

//	----- DEVELOPER MODE PAGE -----
def devMode() {
	def driverVerionText = "TP-Link Kasa Drivers for SmartThings: ${driverVersionsMin()}\n" + "Note: Drivers from the old the original repository will not work with this version of the application."
	def hideInfoDiagDescCont = (true)
	def hideInfoDiagDescStat = (state.currentError == null)
	return dynamicPage(
		name: "devMode", 
		title: "TP-Link Kasa - Developer Page", 
		uninstall: false) {
		section("") {
			paragraph appSmallInfoDesc(), image: getAppImg("kasa_logo.png")
		}
		section("Application Information:", hideable: hideInfoDiagDescCont, hidden: hideInfoDiagDescStat) {
			paragraph title: "Driver Version:", driverVerionText
		}
		section("Help and Feedback:") {
			
		}
	}
}

def getDevices() {
	def currentDevices = getDeviceData()
	state.devices = [:]
	def devices = state.devices
	currentDevices.each {
		def device = [:]
		device["deviceMac"] = it.deviceMac
		device["alias"] = it.alias
		device["deviceModel"] = it.deviceModel
		device["deviceId"] = it.deviceId
		device["appServerUrl"] = it.appServerUrl
		devices << ["${it.deviceMac}": device]
		def isChild = getChildDevice(it.deviceMac)
		if (isChild) {
			isChild.syncAppServerUrl(it.appServerUrl)
		}
		log.info "Device ${it.alias} added to devices array"
	}
}

def addDevices() {
	def tpLinkModel = [:]
	//	Plug-Switch Devices (no energy monitor capability)
	tpLinkModel << ["HS100" : "TP-Link Smart Plug - Kasa Account"]						//	HS100
	tpLinkModel << ["HS105" : "TP-Link Smart Plug - Kasa Account"]						//	HS105
	tpLinkModel << ["HS200" : "TP-Link Smart Switch - Kasa Account"]					//	HS200
	tpLinkModel << ["HS210" : "TP-Link Smart Switch - Kasa Account"]					//	HS210
	tpLinkModel << ["KP100" : "TP-Link Smart Plug - Kasa Account"]						//	KP100
	//	Dimming Plug Devices
	tpLinkModel << ["HS220" : "TP-Link Smart Dimming Switch - Kasa Account"]			//	HS220
	//	Energy Monitor Plugs
	tpLinkModel << ["HS110" : "TP-Link Smart Energy Monitor Plug - Kasa Account"]		//	HS110
	tpLinkModel << ["HS115" : "TP-Link Smart Energy Monitor Plug - Kasa Account"]		//	HS110
	//	Soft White Bulbs
	tpLinkModel << ["KB100" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	KB100
	tpLinkModel << ["LB100" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	LB100
	tpLinkModel << ["LB110" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	LB110
	tpLinkModel << ["KL110" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	KL110
	tpLinkModel << ["LB200" : "TP-Link Smart Soft White Bulb - Kasa Account"]			//	LB200
	//	Tunable White Bulbs
	tpLinkModel << ["LB120" : "TP-Link Smart Tunable White Bulb - Kasa Account"]		//	LB120
	tpLinkModel << ["KL120" : "TP-Link Smart Tunable White Bulb - Kasa Account"]		//	KL120
	//	Color Bulbs
	tpLinkModel << ["KB130" : "TP-Link Smart Color Bulb - Kasa Account"]				//	KB130
	tpLinkModel << ["LB130" : "TP-Link Smart Color Bulb - Kasa Account"]				//	LB130
	tpLinkModel << ["KL130" : "TP-Link Smart Color Bulb - Kasa Account"]				//	KL130
	tpLinkModel << ["LB230" : "TP-Link Smart Color Bulb - Kasa Account"]				//	LB230

	def hub = location.hubs[0]
	def hubId = hub.id
	selectedDevices.each { dni ->
		def isChild = getChildDevice(dni)
		if (!isChild) {
			def device = state.devices.find { it.value.deviceMac == dni }
			def deviceModel = device.value.deviceModel.substring(0,5)
			addChildDevice(
				"ramiran2",
				tpLinkModel["${deviceModel}"], 
				device.value.deviceMac,
				hubId, [
					"label": device.value.alias,
						"name": device.value.deviceModel, 
					"data": [
						"deviceId" : device.value.deviceId,
						"appServerUrl": device.value.appServerUrl,
					]
				]
			)
			log.info "Installed TP-Link $deviceModel with alias ${device.value.alias}"
		}
	}
}

//	----- GET A NEW TOKEN FROM CLOUD -----
def getToken() {
	def hub = location.hubs[0]
	def cmdBody = [
		method: "login",
		params: [
			appType: "Kasa_Android",
			cloudUserName: "${userName}",
			cloudPassword: "${userPassword}",
			terminalUUID: "${hub.id}"
		]
	]
	def getTokenParams = [
		uri: "https://wap.tplinkcloud.com",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getTokenParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			state.TpLinkToken = resp.data.result.token
			log.info "TpLinkToken updated to ${state.TpLinkToken}"
			sendEvent(name: "TokenUpdate", value: "tokenUpdate Successful.")
			if (state.currentError != null) {
				state.currentError = null
			}
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getToken: ${state.currentError}"
			sendEvent(name: "TokenUpdate", value: state.currentError)
		}
	}
}

//	----- GET DEVICE DATA FROM THE CLOUD -----
def getDeviceData() {
	def currentDevices = ""
	def cmdBody = [method: "getDeviceList"]
	def getDevicesParams = [
		uri: "https://wap.tplinkcloud.com?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(getDevicesParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			currentDevices = resp.data.result.deviceList
			if (state.currentError != null) {
				state.currentError = null
			}
			return currentDevices
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getDeviceData: ${state.currentError}"
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in getDeviceData: ${state.currentError}"
		}
	}
}

//	----- SEND DEVICE COMMAND TO CLOUD FOR DH -----
def sendDeviceCmd(appServerUrl, deviceId, command) {
	def cmdResponse = ""
	def cmdBody = [
		method: "passthrough",
		params: [
			deviceId: deviceId, 
			requestData: "${command}"
		]
	]
	def sendCmdParams = [
		uri: "${appServerUrl}/?token=${state.TpLinkToken}",
		requestContentType: 'application/json',
		contentType: 'application/json',
		headers: ['Accept':'application/json; version=1, */*; q=0.01'],
		body : new groovy.json.JsonBuilder(cmdBody).toString()
	]
	httpPostJson(sendCmdParams) {resp ->
		if (resp.status == 200 && resp.data.error_code == 0) {
			def jsonSlurper = new groovy.json.JsonSlurper()
			cmdResponse = jsonSlurper.parseText(resp.data.result.responseData)
			if (state.errorCount != 0) {
				state.errorCount = 0
			}
			if (state.currentError != null) {
				state.currentError = null
				sendEvent(name: "currentError", value: null)
			log.debug "state.errorCount = ${state.errorCount} //	state.currentError = ${state.currentError}"
			}
//			log.debug "state.errorCount = ${state.errorCount} //	state.currentError = ${state.currentError}"
		} else if (resp.status != 200) {
			state.currentError = resp.statusLine
			cmdResponse = "ERROR: ${resp.statusLine}"
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in sendDeviceCmd: ${state.currentError}"
		} else if (resp.data.error_code != 0) {
			state.currentError = resp.data
			cmdResponse = "ERROR: ${resp.data.msg}"
			sendEvent(name: "currentError", value: resp.data)
			log.error "Error in sendDeviceCmd: ${state.currentError}"
		}
	}
	return cmdResponse
}

//	----- INSTALL, UPDATE, INITIALIZE -----
def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	unsubscribe()
	unschedule()
	runEvery5Minutes(checkError)
	schedule("0 30 2 ? * WED", getToken)
	if (selectedDevices) {
		addDevices()
	}
}

//	----- PERIODIC CLOUD MX TASKS -----
def checkError() {
	if (state.currentError == null || state.currentError == "none") {
		log.info "TP-Link Connect did not have any set errors."
		return
	}
	def errMsg = state.currentError.msg
	log.info "Attempting to solve error: ${errMsg}"
	state.errorCount = state.errorCount +1
	if (errMsg == "Token expired" && state.errorCount < 6) {
		sendEvent (name: "ErrHandling", value: "Handle comms error attempt ${state.errorCount}")
		getDevices()
		if (state.currentError == null) {
			log.info "getDevices successful. apiServerUrl updated and token is good."
			return
		}
		log.error "${errMsg} error while attempting getDevices. Will attempt getToken"
		getToken()
		if (state.currentError == null) {
			log.info "getToken successful. Token has been updated."
			getDevices()
			return
		}
	} else {
		log.error "checkError: No auto-correctable errors or exceeded Token request count."
	}
	log.error "checkError residual: ${state.currentError}"
}

//	----- CHILD CALLED TASKS -----
def removeChildDevice(alias, deviceNetworkId) {
	try {
		deleteChildDevice(it.deviceNetworkId)
		sendEvent(name: "DeviceDelete", value: "${alias} deleted")
	} catch (Exception e) {
		sendEvent(name: "DeviceDelete", value: "Failed to delete ${alias}")
	}
}

def gitBranch() { return "master" }
def getAppImg(file) { return "https://raw.githubusercontent.com/ramiran2/TP-Link-Kasa-Device-Manager-SmartThings/${gitBranch()}/images/$file" }
def getWikiPageUrl() { return "https://github.com/ramiran2/TP-Link-Kasa-Device-Manager-SmartThings/wiki" }
def getIssuePageUrl() { return "https://github.com/ramiran2/TP-Link-Kasa-Device-Manager-SmartThings/issues" }
def appInfoDesc()	{
	def str = ""
	str += "TP-Link Kasa Device Manager"
	str += "\n" + "• Version: ${appVersion()}"
	str += "\n" + "• Updated: ${appVerDate()}"
	str += "\n" + "• Author: ${appAuthor()}"
	return str
}
def appSmallInfoDesc()	{
	def strTwo = ""
	strTwo += "TP-Link Kasa Device Manager"
	strTwo += "\n" + "• Version: ${appVersion()}"
	strTwo += "\n" + "• Updated: ${appVerDate()}"
	return strTwo
}