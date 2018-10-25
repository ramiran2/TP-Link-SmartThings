/*
TP-Link Energy Monitor Plug Device Handler, 2018, Version 3

	Copyright 2018 Dave Gutheinz, Anthony Ramirez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the
License. You may obtain a copy of the License at:

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied. See the License for the specific
language governing permissions and limitations under the
License.

Discalimer: This Service Manager and the associated Device
Handlers are in no way sanctioned or supported by TP-Link.
All development is based upon open-source data on the
TP-Link devices; primarily various users on GitHub.com.

	======== Device Type Identifier - Do Not Change These Values =============================*/
	def deviceType()	{ return "Energy Monitor Plug" }									//	Energy Monitor Plug
//	======== TP-Link Account or Local Server Installation ====================================
//	def installType()	{ return "Cloud" }													//	Davegut: Cloud
//	def installType()	{ return "Hub" }													//	Davegut: Hub
	def installType()	{ return "Kasa Account" }											//	Ramiran2: Kasa Account
//	def installType()	{ return "Node Applet" }											//	Ramiran2: Node Applet
//	======== Developer Namespace =============================================================
//	def devNamespace()	{ return "davegut" }												//	Davegut: Developer Namespace
	def devNamespace()	{ return "ramiran2" }												//	Ramiran2: Developer Namespace
//	======== Repository Name =================================================================
//	def gitName()	{ return "SmartThings_Cloud-Based_TP-Link-Plugs-Switches-Bulbs" }		//	Davegut: Repository Name
	def gitName()	{ return "TP-Link-SmartThings" }										//	Ramiran2: Repository Name
//	======== Device Name =====================================================================
//	def devName()	{ return "(${installType()}) TP-Link ${deviceType()}" }					//	Davegut: Device Name
	def devName()	{ return "TP-Link Smart ${deviceType()} - ${installType()}" }			//	Ramiran2: Device Name
//	======== Other System Values =============================================================
	def devAuthor()	{ return "Dave Gutheinz, Anthony Ramirez" }								//	Device Handler Author
	def devVer()	{ return "3.5.0" }														//	Device Handler Version
	def ocfValue()	{ return "oic.d.smartplug" }											//	Open Connectivity Foundation Device Type: Smart Plug
	def vidValue()	{ return "generic-switch-power-energy" }								//	Vendor ID: Switch With Energy Monitoring
//	==========================================================================================

metadata {
	definition (name: "${devName()}", namespace: "${devNamespace()}", author: "${devAuthor()}", ocfDeviceType: "${ocfValue()}", mnmn: "SmartThings", vid: "${vidValue()}") {
		capability "Switch"
		capability "refresh"
		capability "Health Check"
		capability "Power Meter"
		command "getPower"
		capability "Energy Meter"
		command "getEnergyStats"
		attribute "monthTotalE", "string"
		attribute "monthAvgE", "string"
		attribute "weekTotalE", "string"
		attribute "weekAvgE", "string"
		attribute "devVer", "string"
		attribute "devTyp", "string"
	}
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action: "switch.off", icon: "st.Appliances.appliances17", backgroundColor: "#00a0dc",
				nextState: "waiting"
				attributeState "off", label:'${name}', action: "switch.on", icon: "st.Appliances.appliances17", backgroundColor: "#ffffff",
				nextState: "waiting"
				attributeState "waiting", label:'${name}', action: "switch.on", icon: "st.Appliances.appliances17", backgroundColor: "#15EE10",
				nextState: "waiting"
				attributeState "unavailable", label:'unavailable', action: "switch.on", icon: "st.Appliances.appliances17", backgroundColor: "#e86d13",
				nextState: "waiting"
			}
			tileAttribute ("deviceError", key: "SECONDARY_CONTROL") {
				attributeState "deviceError", label: '${currentValue}'
			}
		}
		standardTile("refresh", "capability.refresh", width: 2, height: 1, decoration: "flat") {
			state "default", label: "Refresh", action: "refresh.refresh"
		}
		valueTile("currentPower", "device.power", decoration: "flat", height: 1, width: 2) {
			state "power", label: 'Current Power \n\r ${currentValue} W'
		}
		valueTile("energyToday", "device.energy", decoration: "flat", height: 1, width: 2) {
			state "energy", label: 'Usage Today\n\r${currentValue} WattHr'
		}
		valueTile("monthTotal", "device.monthTotalE", decoration: "flat", height: 1, width: 2) {
			state "monthTotalE", label: '30 Day Total\n\r ${currentValue} KWH'
		}
		valueTile("monthAverage", "device.monthAvgE", decoration: "flat", height: 1, width: 2) {
			state "monthAvgE", label: '30 Day Avg\n\r ${currentValue} KWH'
		}
		valueTile("weekTotal", "device.weekTotalE", decoration: "flat", height: 1, width: 2) {
			state "weekTotalE", label: '7 Day Total\n\r ${currentValue} KWH'
		}
		valueTile("weekAverage", "device.weekAvgE", decoration: "flat", height: 1, width: 2) {
			state "weekAvgE", label: '7 Day Avg\n\r ${currentValue} KWH'
		}
		valueTile("4x1Blank", "default", decoration: "flat", height: 1, width: 4) {
			state "default", label: ''
		}
		main("switch")
		details("switch", "refresh", "4x1Blank", "currentPower", "weekTotal", "monthTotal", "energyToday", "weekAverage", "monthAverage")
	}
	preferences {
		if (installType() == "Node Applet") {
			input ("deviceIP", "text", title: "Device IP", required: true, image: getDevImg("samsunghub.png"))
			input ("gatewayIP", "text", title: "Gateway IP", required: true, image: getDevImg("router.png"))
		}
		input ("refreshRate", "enum", title: "Device Refresh Rate", options: ["1" : "Refresh every minute", "5" : "Refresh every 5 minutes", "10" : "Refresh every 10 minutes", "15" : "Refresh every 15 minutes", "30" : "Refresh every 30 minutes"], image: getDevImg("refresh.png"))
	}
}

//	===== Update when installed or setting changed =====
def initialize() {
	log.info "Initialized ${device.label}..."
	sendEvent(name: "devVer", value: devVer(), displayed: false)
	sendEvent(name: "devTyp", value: deviceType(), displayed: false)
	sendEvent(name: "DeviceWatch-Enroll", value: groovy.json.JsonOutput.toJson(["protocol" : "cloud", "scheme" : "untracked"]), displayed: false)
}

def ping() {
	refresh()
}

def installed() {
	updated()
}

def updated() {
	log.info "Updated ${device.label}..."
	state.emon = metadata.definition.energyMonitor
	state.emeterText = "emeter"
	state.getTimeText = "time"
	unschedule()
	//	Update Refresh Rate Preference
	if (refreshRate) {
		setRefreshRate(refreshRate)
	} else {
		setRefreshRate(30)
	}
	sendEvent(name: "devVer", value: devVer(), displayed: false)
	sendEvent(name: "devTyp", value: deviceType(), displayed: false)
	schedule("0 05 0 * * ?", setCurrentDate)
	schedule("0 10 0 * * ?", getEnergyStats)
	setCurrentDate()
	runIn(2, refresh)
	runIn(7, getEnergyStats)
}

void uninstalled() {
	if (installType() == "Kasa Account") {
		def alias = device.label
		log.debug "Removing device ${alias} with DNI = ${device.deviceNetworkId}"
		parent.removeChildDevice(alias, device.deviceNetworkId)
	}
}

//	===== Basic Plug Control/Status =====
def on() {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 1}}}', "deviceCommand", "commandResponse")
	runIn(2, refresh)
}

def off() {
	sendCmdtoServer('{"system":{"set_relay_state":{"state": 0}}}', "deviceCommand", "commandResponse")
	runIn(2, refresh)
}

def getSystemInfo() {
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "refreshResponse")
	runIn(2, getPower)
}

def refresh(){
	sendCmdtoServer('{"system":{"get_sysinfo":{}}}', "deviceCommand", "refreshResponse")
	runIn(2, getPower)
	runIn(7, getConsumption)
}

def refreshResponse(cmdResponse){
	def status = cmdResponse.system.get_sysinfo.relay_state
	if (installType() == "Node Applet") {
		if ("${deviceIP}" =~ null && "${gatewayIP}" =~ null) {
			sendEvent(name: "switch", value: "unavailable", descriptionText: "Please input Device IP / Gateway IP")
			sendEvent(name: "deviceError", value: "Please input Device IP / Gateway IP")
			sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
		} else {
			if (status == 1) {
				status = "on"
			} else {
				status = "off"
			}
			sendEvent(name: "switch", value: status)
		}
	} else {
		if (status == 1) {
			status = "on"
		} else {
			status = "off"
		}
		sendEvent(name: "switch", value: status)
	}
	log.info "${device.name} ${device.label}: Power: ${status}"
	sendEvent(name: "switch", value: status)
}

//	===== Get Current Energy Data =====
def getPower(){
	sendCmdtoServer("""{"${state.emeterText}":{"get_realtime":{}}}""", "deviceCommand", "energyMeterResponse")
}

def energyMeterResponse(cmdResponse) {
	def realtime = cmdResponse["emeter"]["get_realtime"]
	if (realtime.power == null) {
		state.powerScale = "power_mw"
		state.energyScale = "energy_wh"
	} else {
		state.powerScale = "power"
		state.energyScale = "energy"
	}
	def powerConsumption = realtime."${state.powerScale}"
		if (state.powerScale == "power_mw") {
			powerConsumption = Math.round(powerConsumption/10) / 100
		} else {
		powerConsumption = Math.round(100*powerConsumption) / 100
		}
	sendEvent(name: "power", value: powerConsumption)
	log.info "$device.name $device.label: Updated CurrentPower to $powerConsumption"
}

//	===== Get Today's Consumption =====
def getConsumption(){
	sendCmdtoServer("""{"${state.emeterText}":{"get_daystat":{"month": ${state.monthToday}, "year": ${state.yearToday}}}}""", "emeterCmd", "useTodayResponse")
}

def useTodayResponse(cmdResponse) {
	def wattHrToday
	def wattHrData
	def dayList = cmdResponse["emeter"]["get_daystat"].day_list
	for (int i = 0; i < dayList.size(); i++) {
		wattHrData = dayList[i]
		if(wattHrData.day == state.dayToday) {
			wattHrToday = wattHrData."${state.energyScale}"
		}
	}
	if (state.powerScale == "power") {
		wattHrToday = Math.round(1000*wattHrToday)
	}
	sendEvent(name: "energy", value: wattHrToday)
	log.info "$device.name $device.label: Updated Usage Today to ${wattHrToday}"
}

//	===== Get Weekly and Monthly Stats =====
def getEnergyStats() {
	state.monTotEnergy = 0
	state.monTotDays = 0
	state.wkTotEnergy = 0
	state.wkTotDays = 0
	sendCmdtoServer("""{"${state.emeterText}":{"get_daystat":{"month": ${state.monthToday}, "year": ${state.yearToday}}}}""", "emeterCmd", "engrStatsResponse")
	runIn(4, getPrevMonth)
}

def getPrevMonth() {
	def prevMonth = state.monthStart
	if (state.monthToday == state.monthStart) {
		//	If all of the data is in this month, do not request previous month.
		//	This will occur when the current month is 31 days.
		return
	} else if (state.monthToday - 2 == state.monthStart) {
		//	If the start month is 2 less than current, we must handle
		//	the data to get a third month - January.
		state.handleFeb = "yes"
		prevMonth = prevMonth + 1
		runIn(4, getJan)
	}
	sendCmdtoServer("""{"${state.emeterText}":{"get_daystat":{"month": ${prevMonth}, "year": ${state.yearStart}}}}""", "emeterCmd", "UseJanWatts")
}

def getJan() {
//	Gets January data on March 1 and 2. Only access if current month = 3
//	and start month = 1
	sendCmdtoServer("""{"${state.emeterText}":{"get_daystat":{"month": ${state.monthStart}, "year": ${state.yearStart}}}}""", "emeterCmd", "engrStatsResponse")
}

def engrStatsResponse(cmdResponse) {
/*	
	This method parses up to two energy status messages from the device,
	adding the energy for the previous 30 days and week, ignoring the
	current day. It then calculates the 30 and 7 day average formatted
	in kiloWattHours to two decimal places.
*/
	def dayList = cmdResponse[state.emeterText]["get_daystat"].day_list
	if (!dayList[0]) {
		log.info "$device.name $device.label: Month has no energy data."
		return
	}
	def monTotEnergy = state.monTotEnergy
	def wkTotEnergy = state.wkTotEnergy
	def monTotDays = state.monTotDays
	def wkTotDays = state.wkTotDays
	def startDay = state.dayStart
	def dataMonth = dayList[0].month
	if (dataMonth == state.monthToday) {
		for (int i = 0; i < dayList.size(); i++) {
			def energyData = dayList[i]
			monTotEnergy += energyData."${state.energyScale}"
			monTotDays += 1
			if (state.dayToday < 8 || energyData.day >= state.weekStart) {
				wkTotEnergy += energyData."${state.energyScale}"
				wkTotDays += 1
			}
			if(energyData.day == state.dayToday) {
				monTotEnergy -= energyData."${state.energyScale}"
				wkTotEnergy -= energyData."${state.energyScale}"
				monTotDays -= 1
				wkTotDays -= 1
			}
		}
	} else if (state.handleFeb == "yes" && dataMonth == 2) {
		startDay = 1
		for (int i = 0; i < dayList.size(); i++) {
			def energyData = dayList[i]
			if (energyData.day >= startDay) {
				monTotEnergy += energyData."${state.energyScale}"
				monTotDays += 1
			}
			if (energyData.day >= state.weekStart && state.dayToday < 8) {
				wkTotEnergy += energyData."${state.energyScale}"
				wkTotDays += 1
			}
		}
	} else if (state.handleFeb == "yes" && dataMonth == 1) {
		for (int i = 0; i < dayList.size(); i++) {
			def energyData = dayList[i]
			if (energyData.day >= startDay) {
				monTotEnergy += energyData."${state.energyScale}"
				monTotDays += 1
			}
			state.handleFeb = ""
		}
	} else {
		for (int i = 0; i < dayList.size(); i++) {
			def energyData = dayList[i]
			if (energyData.day >= startDay) {
				monTotEnergy += energyData."${state.energyScale}"
				monTotDays += 1
			}
			if (energyData.day >= state.weekStart && state.dayToday < 8) {
				wkTotEnergy += energyData."${state.energyScale}"
				wkTotDays += 1
			}
		}
	}
	state.monTotDays = monTotDays
	state.monTotEnergy = monTotEnergy
	state.wkTotEnergy = wkTotEnergy
	state.wkTotDays = wkTotDays
	log.info "$device.name $device.label: Update 7 and 30 day energy consumption statistics"
	if (monTotDays == 0) {
		//	Aviod divide by zero on 1st of month
		monTotDays = 1
		wkTotDays = 1
	}
	def monAvgEnergy =monTotEnergy/monTotDays
	def wkAvgEnergy = wkTotEnergy/wkTotDays
	if (state.powerScale == "power_mw") {
		monAvgEnergy = Math.round(monAvgEnergy/10)/100
		wkAvgEnergy = Math.round(wkAvgEnergy/10)/100
		monTotEnergy = Math.round(monTotEnergy/10)/100
		wkTotEnergy = Math.round(wkTotEnergy/10)/100
	} else {
		monAvgEnergy = Math.round(100*monAvgEnergy)/100
		wkAvgEnergy = Math.round(100*wkAvgEnergy)/100
		monTotEnergy = Math.round(100*monTotEnergy)/100
		wkTotEnergy = Math.round(100*wkTotEnergy)/100
	}
	sendEvent(name: "monthTotalE", value: monTotEnergy)
	sendEvent(name: "monthAvgE", value: monAvgEnergy)
	sendEvent(name: "weekTotalE", value: wkTotEnergy)
	sendEvent(name: "weekAvgE", value: wkAvgEnergy)
}

//	===== Obtain Week and Month Data =====
def setCurrentDate() {
	sendCmdtoServer('{"time" :{"get_time" :null}}', "deviceCommand", "currentDateResponse")
}

def currentDateResponse(cmdResponse) {
	def currDate = cmdResponse["time"]["get_time"]
	state.dayToday = currDate.mday.toInteger()
	state.monthToday = currDate.month.toInteger()
	state.yearToday = currDate.year.toInteger()
	def dateToday = Date.parse("yyyy-MM-dd", "${currDate.year}-${currDate.month}-${currDate.mday}")
	def monStartDate = dateToday - 30
	def wkStartDate = dateToday - 7
	state.dayStart = monStartDate[Calendar.DAY_OF_MONTH].toInteger()
	state.monthStart = monStartDate[Calendar.MONTH].toInteger() + 1
	state.yearStart = monStartDate[Calendar.YEAR].toInteger()
	state.weekStart = wkStartDate[Calendar.DAY_OF_MONTH].toInteger()
}

//	===== Send the Command =====
private sendCmdtoServer(command, hubCommand, action) {
	try {
		if (installType() == "Kasa Account") {
			sendCmdtoCloud(command, hubCommand, action)
		} else {
			if ("${deviceIP}" =~ null && "${gatewayIP}" =~ null) {
				sendEvent(name: "switch", value: "unavailable", descriptionText: "Please input Device IP / Gateway IP")
				sendEvent(name: "deviceError", value: "Please input Device IP / Gateway IP")
				sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
			} else {
				sendCmdtoHub(command, hubCommand, action)
			}
		}
	} catch (ex) {
		log.error "Sending Command Exception: ", ex
	}
}

private sendCmdtoCloud(command, hubCommand, action){
	def appServerUrl = getDataValue("appServerUrl")
	def deviceId = getDataValue("deviceId")
	def cmdResponse = parent.sendDeviceCmd(appServerUrl, deviceId, command)
	String cmdResp = cmdResponse.toString()
	if (cmdResp.substring(0,5) == "ERROR"){
		def errMsg = cmdResp.substring(7,cmdResp.length())
		log.error "${device.name} ${device.label}: ${errMsg}"
		sendEvent(name: "switch", value: "unavailable", descriptionText: errMsg)
		sendEvent(name: "deviceError", value: errMsg)
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
		action = ""
	} else {
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
		sendEvent(name: "deviceError", value: "OK")
	}
	actionDirector(action, cmdResponse)
}

private sendCmdtoHub(command, hubCommand, action){
	def headers = [:]
	headers.put("HOST", "$gatewayIP:8082")	//	Same as on Hub.
	headers.put("tplink-iot-ip", deviceIP)
	headers.put("tplink-command", command)
	headers.put("action", action)
	headers.put("command", hubCommand)
	sendHubCommand(new physicalgraph.device.HubAction([headers: headers], device.deviceNetworkId, [callback: hubResponseParse]))
}

def hubResponseParse(response) {
	def action = response.headers["action"]
	def cmdResponse = parseJson(response.headers["cmd-response"])
	if (cmdResponse == "TcpTimeout") {
		log.error "$device.name $device.label: Communications Error"
		sendEvent(name: "switch", value: "offline", descriptionText: "ERROR at hubResponseParse TCP Timeout")
		sendEvent(name: "deviceError", value: "TCP Timeout in Hub")
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false, isStateChange: true)
	} else {
		sendEvent(name: "deviceError", value: "OK")
		sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false, isStateChange: true)
		actionDirector(action, cmdResponse)
	}
}

def actionDirector(action, cmdResponse) {
	switch(action) {
		case "commandResponse" :
			refresh()
			break
		case "refreshResponse" :
			refreshResponse(cmdResponse)
			break
		case "energyMeterResponse" :
			energyMeterResponse(cmdResponse)
			break
		case "useTodayResponse" :
			useTodayResponse(cmdResponse)
			break
		case "currentDateResponse" :
			currentDateResponse(cmdResponse)
			break
		case "engrStatsResponse" :
			engrStatsResponse(cmdResponse)
			break
		default:
			log.debug "Interface Error.	See SmartApp and Device error message."
	}
}

//	===== Child / Parent Interchange =====
def syncAppServerUrl(newAppServerUrl) {
	updateDataValue("appServerUrl", newAppServerUrl)
	log.info "Updated appServerUrl for ${device.name} ${device.label}"
}

def setLightTransTime(lightTransTime) {
	return
}

def setRefreshRate(refreshRate) {
	switch(refreshRate) {
		case "1" :
			runEvery1Minute(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every minute"
			break
		case "5" :
			runEvery5Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 5 minutes"
			break
		case "10" :
			runEvery10Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 10 minutes"
			break
		case "15" :
			runEvery15Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 15 minutes"
			break
		case "30" :
			runEvery30Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 30 minutes"
			break
		default:
			runEvery30Minutes(refresh)
			log.info "${device.name} ${device.label} Refresh Scheduled for every 30 minutes"
	}
}

//	======== GitHub Values =====================================================================================================================================================================================
	def getDevImg(imgName)	{ return "https://raw.githubusercontent.com/${gitPath()}/images/$imgName" }
	def gitBranch()	{ return "master" }
	def gitRepo()		{ return "${devNamespace()}/${gitName()}" }
	def gitPath()		{ return "${gitRepo()}/${gitBranch()}"}
	def betaMarker()	{ return false }
//	============================================================================================================================================================================================================