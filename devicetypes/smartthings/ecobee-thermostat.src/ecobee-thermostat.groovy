/**
 *  Based on original version Copyright 2015 SmartThings
 *  Additions Copyright 2016 Sean Kendall Schneyer
 *  Additions Copyright 2017 Barry A. Burke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *	Ecobee Thermostat
 *
 *	Author: SmartThings
 *	Date: 2013-06-13
 *
 * 	Updates by Sean Kendall Schneyer <smartthings@linuxbox.org>
 * 	Date: 2015-12-23
 *
 *	Updates by Barry A. Burke (storageanarchy@gmail.com)
 *  https://github.com/SANdood/Ecobee
 *
 *  See Changelog for change history 
 *
 *	1.0.0  - Preparation for General Release
 *  1.0.1  - Added support for Thermostat Offline from Ecobee Cloud
 *	1.0.2  - Fixed intermittent update of humidity
 *  1.0.3  - Added Health Check support & Thermostat date/time display
 *	1.0.4  - Fixed "Auto" as default program
 *	1.0.5  - Fixed handling of resumeProgram and setThermostatProgram
 *
 */

def getVersionNum() { return "1.0.5" }
private def getVersionLabel() { return "Ecobee Thermostat Version ${getVersionNum()}" }
import groovy.json.JsonSlurper
 
metadata {
	definition (name: "Ecobee Thermostat", namespace: "smartthings", author: "SmartThings") {
		capability "Actuator"
		capability "Thermostat"
        capability "Sensor"
		capability "Refresh"
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		// capability "Presence Sensor"
        capability "Motion Sensor"
        
        // Extended Set of Thermostat Capabilities
        capability "Thermostat Cooling Setpoint"
		capability "Thermostat Fan Mode"
		capability "Thermostat Heating Setpoint"
		capability "Thermostat Mode"
		capability "Thermostat Operating State"
		capability "Thermostat Setpoint"   
        capability "Health Check"

		command "setTemperature"
        command "auxHeatOnly"

		command "generateEvent"
		command "raiseSetpoint"
		command "lowerSetpoint"
		command "resumeProgram"
		command "switchMode"
        
        command "setThermostatProgram"
        command "setFanMinOnTime"
        command "setVacationFanMinOnTime"
        command "deleteVacation"
        command "home"
        command "present"

// Unfortunately we cannot overload the internal definition of 'sleep()', and calling this will silently fail (actually, it does a
// "sleep(0)")
//		command "sleep"
        command "asleep"
        command "night"				// this is probably the appropriate SmartThings device command to call, matches ST mode
        command "away"
        
        command "fanOff"  			// Missing from the Thermostat standard capability set
        command "noOp" 				// Workaround for formatting issues 
        command "setStateVariable"

		// Capability "Thermostat"
        attribute "temperatureScale", "string"
		attribute "thermostatSetpoint","number"
		attribute "thermostatStatus","string"
        attribute "apiConnected","string"
        attribute "ecobeeConnected", "string"
        
		attribute "currentProgramName", "string"
        attribute "currentProgramId","string"
		attribute "currentProgram","string"
		attribute "scheduledProgramName", "string"
        attribute "scheduledProgramId","string"
		attribute "scheduledProgram","string"
        attribute "weatherSymbol", "string"        
        attribute "debugEventFromParent","string"
        attribute "logo", "string"
        attribute "timeOfDay", "enum", ["day", "night"]
        attribute "lastPoll", "string"
        
		attribute "equipmentStatus", "string"
        attribute "humiditySetpoint", "string"
        attribute "weatherTemperature", "number"
		attribute "decimalPrecision", "number"
		attribute "temperatureDisplay", "string"
		attribute "equipmentOperatingState", "string"
        attribute "coolMode", "string"
		attribute "heatMode", "string"
        attribute "autoMode", "string"
		attribute "heatStages", "number"
		attribute "coolStages", "number"
		attribute "hasHeatPump", "string"
        attribute "hasForcedAir", "string"
        attribute "hasElectric", "string"
        attribute "hasBoiler", "string"
        attribute "hasHumidifier", "string"
        attribute "hasDehumidifier", "string"
		attribute "auxHeatMode", "string"
        attribute "motion", "string"
		attribute "heatRangeHigh", "number"
		attribute "heatRangeLow", "number"
		attribute "coolRangeHigh", "number"
		attribute "coolRangeLow", "number"
		attribute "heatRange", "string"
		attribute "coolRange", "string"
		attribute "thermostatHold", "string"
		attribute "holdStatus", "string"
        attribute "heatDifferential", "number"
        attribute "coolDifferential", "number"
        attribute "fanMinOnTime", "number"
        attribute "programsList", "enum"
        attribute "thermostatOperatingStateDisplay", "string"
        attribute "thermostatTime", "string"
		
		// attribute "debugLevel", "number"
		
        attribute "smart1", "string"
        attribute "smart2", "string"
        attribute "smart3", "string"
        attribute "smart4", "string"
        attribute "smart5", "string"
        attribute "smart6", "string"
        attribute "smart7", "string"
        attribute "smart8", "string"
        attribute "smart9", "string"
        attribute "smart10", "string"
	}

	simulator { }

    tiles(scale: 2) {      
              
		multiAttributeTile(name:"temperatureDisplay", type:"thermostat", width:6, height:4) {
			tileAttribute("device.temperatureDisplay", key: "PRIMARY_CONTROL") {
				attributeState("default", label:'${currentValue}', unit:"dF")
			}
			tileAttribute("device.temperature", key: "VALUE_CONTROL") {
                attributeState("default", action: "setTemperature")
			}
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'${currentValue}%', unit:"%")
			}
			tileAttribute('device.thermostatOperatingStateDisplay', key: "OPERATING_STATE") {
				attributeState('idle', backgroundColor:"#d28de0")			// ecobee purple/magenta
                attributeState('fan only', backgroundColor:"66cc00")		// ecobee green
				attributeState('heating', backgroundColor:"#ff9c14")		// ecobee flame orange
				attributeState('cooling', backgroundColor:"#2db9e7")		// ecobee snowflake blue
                attributeState('heating (smart recovery)', backgroundColor:"#ff9c14")		// ecobee flame orange
                attributeState('cooling (smart recovery)', backgroundColor:"#2db9e7")		// ecobee snowflake blue
                attributeState('offline', backgroundColor:"#ff4d4d")
                attributeState('default', label: 'idle', backgroundColor:"#d28de0")
			}
			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", label:'${name}')
				attributeState("heat", label:'${name}')
				attributeState("cool", label:'${name}')
                attributeState("auto", label:'${name}')
			}
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
            	attributeState("default", label:'${currentValue}°', unit:"dF")
            }
			tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
				attributeState("default", label:'${currentValue}°', unit:"dF")
			}
		} // End multiAttributeTile
        
        // Workaround until they fix the Thermostat multiAttributeTile. Only use this one OR the above one, not both
        multiAttributeTile(name:"summary", type: "lighting", width: 6, height: 4) {
        	tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState("temperature", label:'${currentValue}°', unit:"dF",
				backgroundColors: getTempColors())
			}
			tileAttribute("device.temperature", key: "VALUE_CONTROL") {
                attributeState("default", action: "setTemperature")
			}
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'${currentValue}%', unit:"%")
			}
			tileAttribute("device.thermostatOperatingStateDisplay", key: "OPERATING_STATE") {
				attributeState("idle", backgroundColor:"#d28de0")			// ecobee purple/magenta
                attributeState("fan only", backgroundColor:"#66cc00")		// ecobee green
				attributeState("heating", backgroundColor:"#ff9c14")		// ecobee snowflake blue
				attributeState("cooling", backgroundColor:"#2db9e7")		// ecobee flame orange
                attributeState('heating (smart recovery)', backgroundColor:"#ff9c14")		// ecobee flame orange
                attributeState('cooling (smart recovery)', backgroundColor:"#2db9e7")		// ecobee snowflake blue
                attributeState('offline', backgroundColor:"#ff4d4d")
                attributeState('default', label: 'idle', backgroundColor:"#d28de0")
			}
			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState("off", label:'${name}')
				attributeState("heat", label:'${name}')
				attributeState("cool", label:'${name}')
                attributeState("auto", label:'${name}')
			}
            tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
            	attributeState("default", label:'${currentValue}°', unit:"dF")
            }
			tileAttribute("device.coolingSetpoint", key: "COOLING_SETPOINT") {
				attributeState("default", label:'${currentValue}°', unit:"dF")
			}
        }

        // Show status of the API Connection for the Thermostat
		standardTile("apiStatus", "device.apiConnected", width: 1, height: 1) {
        	state "full", label: "API", backgroundColor: "#00A0D3", icon: "st.contact.contact.closed"
            state "warn", label: "API ", backgroundColor: "#FFFF33", icon: "st.contact.contact.open"
            state "lost", label: "API ", backgroundColor: "#ffa81e", icon: "st.contact.contact.open"
		}

		valueTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: false, icon: "st.Home.home1") {
			state("temperature", label:'${currentValue}°', unit:"F", backgroundColors: getTempColors())
		}
        
        // these are here just to get the colored icons to diplay in the Recently log in the Mobile App
        valueTile("heatingSetpointColor", "device.heatingSetpoint", width: 2, height: 2, canChangeIcon: false, icon: "st.Home.home1") {
			state("heatingSetpoint", label:'${currentValue}°', unit:"F", backgroundColor:"#ff9c14")
		}
        valueTile("coolingSetpointColor", "device.coolingSetpoint", width: 2, height: 2, canChangeIcon: false, icon: "st.Home.home1") {
			state("coolingSetpoint", label:'${currentValue}°', unit:"F", backgroundColor:"#2db9e7")
		}
        valueTile("thermostatSetpointColor", "device.thermostatSetpoint", width: 2, height: 2, canChangeIcon: false, icon: "st.Home.home1") {
			state("thermostatSetpoint", label:'${currentValue}°', unit:"F",	backgroundColors: getTempColors())
		}
        valueTile("weatherTempColor", "device.weatherTemperature", width: 2, height: 2, canChangeIcon: false, icon: "st.Home.home1") {
			state("weatherTemperature", label:'${currentValue}°', unit:"F",	backgroundColors: getStockTempColors())		// use Fahrenheit scale so that outdoor temps register
		}
		
		standardTile("mode", "device.thermostatMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "off", action:"thermostat.heat", label: "Set Mode", nextState: "updating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_off.png"
			state "heat", action:"thermostat.cool",  label: "Set Mode", nextState: "updating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_heat.png"
			state "cool", action:"thermostat.auto",  label: "Set Mode", nextState: "updating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_cool.png"
			state "auto", action:"thermostat.off",  label: "Set Mode", nextState: "updating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_auto.png"
            // Not included in the button loop, but if already in "auxHeatOnly" pressing button will go to "auto"
			state "auxHeatOnly", action:"thermostat.auto", icon: "st.thermostat.emergency-heat"
			state "updating", label:"Working", icon: "st.secondary.secondary"
		}
        
        standardTile("modeShow", "device.thermostatMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "off", action:"noOp", label: "Off", nextState: "off", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_off.png"
			state "heat", action:"noOp",  label: "Heat", nextState: "heat", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_heat.png"
			state "cool", action:"noOp",  label: "Cool", nextState: "cool", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_cool.png"
			state "auto", action:"noOp",  label: "Auto", nextState: "auto", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_auto.png"
            // Not included in the button loop, but if already in "auxHeatOnly" pressing button will go to "auto"
			state "auxHeatOnly", action:"noOp", icon: "st.thermostat.emergency-heat"
			state "updating", label:"Working", icon: "st.secondary.secondary"
		}
        
        // TODO Use a different color for the one that is active
		standardTile("setModeHeat", "device.thermostatMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {			
			state "heat", action:"thermostat.heat",  label: "Heat", nextState: "updating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_heat.png"
			state "updating", label:"Working...", icon: "st.secondary.secondary"
		}
		standardTile("setModeCool", "device.thermostatMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {			
			state "cool", action:"thermostat.cool",  label: "Cool", nextState: "updating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_cool.png"
			state "updating", label:"Working...", icon: "st.secondary.secondary"
		}        
		standardTile("setModeAuto", "device.thermostatMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {			
			state "auto", action:"thermostat.auto",  label: "Auto", nextState: "updating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_auto.png"
			state "updating", label:"Working...", icon: "st.secondary.secondary"
		}
		standardTile("setModeOff", "device.thermostatMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {			
			state "off", action:"thermostat.off", label: "Off", nextState: "updating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/systemmode_off.png"
			state "updating", label:"Working...", icon: "st.secondary.secondary"
		}

		standardTile("fanModeLabeled", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "on", label:'On', action:"noOp", nextState: "on", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
            state "auto", label:'Auto', action:"noOp", nextState: "auto", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
            state "off", label:'Off', action:"noOp", nextState: "auto", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan.png"
			state "circulate", label:'Circulate', action:"noOp", nextState: "circulate", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on.png"
            state "updating", label:"Working", icon: "st.secondary.secondary"
		}
        
        standardTile("fanOffButton", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "off", label:"Fan Off", action:"fanOff", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_big_nolabel.png"
            state "updating", label:"Working", icon: "st.secondary.secondary"
		}

		standardTile("fanCirculate", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "circulate", label:"Fan Circulate", action:"thermostat.fanCirculate", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_big_on_nolabel.png"
            state "updating", label:"Working", icon: "st.secondary.secondary"
		}
        
		standardTile("fanMode", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "on", action:"thermostat.fanAuto", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_big_on_solid_nolabel.png"
            state "auto", action:"thermostat.fanOn", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_big_on_nolabel.png"
            state "off", action:"thermostat.fanAuto", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_big_nolabel.png"
			state "circulate", action:"thermostat.fanAuto", nextState: "updating", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_big_on_nolabel.png"
            state "updating", label:"Working", icon: "st.secondary.secondary"
		}
        standardTile("fanModeAutoSlider", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
        	state "on", action:"thermostat.fanAuto", nextState: "auto", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/fanmode_auto_slider_off.png"
            state "auto", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/fanmode_auto_slider_on.png"
        }
		standardTile("fanModeOnSlider", "device.thermostatFanMode", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
        	state "auto", action:"thermostat.fanOn", nextState: "auto", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/fanmode_on_slider_off.png"
            state "on", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/fanmode_on_slider_on.png"
        }
        
		standardTile("upButtonControl", "device.thermostatSetpoint", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
			state "setpoint", action:"raiseSetpoint", icon:"st.thermostat.thermostat-up"
		}
		valueTile("thermostatSetpoint", "device.thermostatSetpoint", width: 2, height: 2, decoration: "flat") {
			state "thermostatSetpoint", label:'${currentValue}°',
				backgroundColors: getTempColors()
		}
		valueTile("currentStatus", "device.thermostatStatus", height: 2, width: 4, decoration: "flat") {
			state "thermostatStatus", label:'${currentValue}', backgroundColor:"#ffffff"
		}
		standardTile("downButtonControl", "device.thermostatSetpoint", height: 1, width: 2, inactiveLabel: false, decoration: "flat") {
			state "setpoint", action:"lowerSetpoint", icon:"st.thermostat.thermostat-down"
		}
		controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 4, inactiveLabel: false, range: getSliderRange() /* "(15..85)" */ ) {
			state "setHeatingSetpoint", action:"thermostat.setHeatingSetpoint",  backgroundColor:"#ff9c14", unit: 'C'
		}
		valueTile("heatingSetpoint", "device.heatingSetpoint", height: 1, width: 1, inactiveLabel: false, decoration: "flat") {
			state "heat", label:'${currentValue}°'//, unit:"F", backgroundColor:"#ff9c14"
		}
		controlTile("coolSliderControl", "device.coolingSetpoint", "slider", height: 1, width: 4, inactiveLabel: false, range: getSliderRange() /* "(15..85)" */ ) {
			state "setCoolingSetpoint", action:"thermostat.setCoolingSetpoint", backgroundColor:"#2db9e7", unit: 'C'
		}
		valueTile("coolingSetpoint", "device.coolingSetpoint", width: 1, height: 1, inactiveLabel: false, decoration: "flat") {
			state "cool", label:'${currentValue}°' //, unit:"F", backgroundColor:"#2db9e7"
		}
		standardTile("refresh", "device.thermostatMode", width: 2, height: 2,inactiveLabel: false, decoration: "flat") {
            state "default", action:"refresh.refresh", label: "Refresh", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/header_ecobeeicon_blk.png"
		}
        
        standardTile("resumeProgram", "device.resumeProgram", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "resume", action:"resumeProgram", nextState: "updating", label:'Resume', icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/action_resume_program.png"
            state "cancel", action:"cancelVacation", nextState: "updating", label:'Cancel', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_vacation_airplane_yellow.png"
			state "updating", label:"Working", icon: "st.motion.motion.inactive"
		}
        
        // TODO: Add icons and handling for Ecobee Comfort Settings
        standardTile("currentProgramIcon", "device.currentProgramName", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "Home", action:"noOp", label: 'Home', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png"
			state "Away", action:"noOp", label: 'Away', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png"
            state "Sleep", action:"noOp", label: 'Sleep', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue.png"
            state "Auto", action:"noOp", label: 'Auto', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
            state "Auto Away", action:"noOp", label: 'Auto Away', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png" // Fix to auto version
            state "Auto Home", action:"noOp", label: 'Auto Home', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png" // Fix to auto
            state "Hold", action:"noOp", label: "Hold Activated", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
            state "Hold: Fan", action:"noOp", label: "Hold: Fan", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_fan_on_solid.png"
            state "Hold: Home", action:"noOp", label: 'Hold: Home', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue_solid.png"
            state "Hold: Away", action:"noOp", label: 'Hold: Away',  icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue_solid.png"
            state "Hold: Sleep", action:"noOp", label: 'Hold: Sleep',  icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue_solid.png"
      		state "Vacation", action: "noOp", label: 'Vacation', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_vacation_airplane_yellow.png"
      		state "Offline", action: "noOp", label: 'Offline', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_black_dot.png"
            state "default", action:"noOp", label: '${currentValue}', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_generic_chair_blue.png"
            
		}        
        
        valueTile("currentProgram", "device.currentProgramName", height: 2, width: 4, inactiveLabel: false, decoration: "flat") {
			state "default", label:'Comfort Setting:\n${currentValue}' 
		}
        
		standardTile("setHome", "device.setHome", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "home", action:"home", nextState: "updating", label:'Home', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_home_blue.png"
			state "updating", label:"Working...", icon: "st.motion.motion.inactive"
		}
        
        standardTile("setAway", "device.setAway", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "away", action:"away", nextState: "updating", label:'Away', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_away_blue.png"
			state "updating", label:"Working...", icon: "st.motion.motion.inactive"
		}

        standardTile("setSleep", "device.setSleep", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			// state "sleep", action:"sleep", nextState: "updating", label:'Set Sleep', icon:"st.Bedroom.bedroom2"
			// can't call "sleep()" because of conflict with internal definition (it silently fails)
            state "sleep", action:"night", nextState: "updating", label:'Sleep', icon:"https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/schedule_asleep_blue.png"
			state "updating", label:"Working...", icon: "st.motion.motion.inactive"
		}

        standardTile("operatingState", "device.thermostatOperatingState", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "idle", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_idle_purple.png"
            state "fan only", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "heating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_heat.png"
			state "cooling", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_cool.png"
            state "offline", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
            state "default", label: '${currentValue}', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png"
		}
        
       	standardTile("operatingStateDisplay", "device.thermostatOperatingStateDisplay", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "idle", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_idle_purple.png"
            state "fan only", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "heating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_heat.png"
			state "cooling", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_cool.png"
            state "heating (smart recovery)", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_heat.png"
			state "cooling (smart recovery)", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_cool.png"
            state "offline", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
            state "default", label: '${currentValue}', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png"
		}
			
		standardTile("equipmentState", "device.equipmentOperatingState", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "idle", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/systemmode_idle_purple.png"
            state "fan only", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_fan_on_solid.png"
			state "emergency", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_emergency.png"
            state "heat pump", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_heat.png"
            state "heat 1", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_1.png"
			state "heat 2", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2.png"
			state "heat 3", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3.png"
			state "heat pump 2", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2.png"
			state "heat pump 3", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3.png"
			state "cool 1", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_1.png"
			state "cool 2", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_2.png"
			state "heating", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_heat.png"
			state "cooling", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_cool.png"
			state "emergency hum", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_emergency+humid.png"
            state "heat pump hum", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_heat+humid.png"
            state "heat 1 hum", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_1+humid.png"
			state "heat 2 hum", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2+humid.png"
			state "heat 3 hum", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3+humid.png"
			state "heat pump 2 hum", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_2+humid.png"
			state "heat pump 3 hum", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_heat_3+humid.png"
			state "cool 1 deh", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_1-humid.png"
			state "cool 2 deh", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_cool_2-humid.png"
			state "heating hum", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_heat+humid.png"
			state "cooling deh", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_cool-humid.png"
            state "humidifier", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_humidifier_only.png"
            state "dehumidifier", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/operatingstate_dehumidifier_only.png"
            state "offline", icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/black_dot_only.png"
            state "default", action:"noOp", label: '${currentValue}', icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/blank.png"
		}

        valueTile("humidity", "device.humidity", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state("default", label: '${currentValue}%', unit: "humidity", backgroundColor:"#d28de0")
		}
        
        standardTile("motionState", "device.motion", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "active", action:"noOp", nextState: "active", label:"Motion", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_motion.png"
			state "inactive", action: "noOp", nextState: "inactive", label:"No Motion", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_nomotion.png"
            state "not supported", action: "noOp", nextState: "not supported", label: "N/A", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/notsupported_x.png"
			state "unknown", action: "noOp", label:"Offline", nextState: "unknown", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/motion_sensor_noconnection.png"
		}

        // Weather Tiles and other Forecast related tiles
		standardTile("weatherIcon", "device.weatherSymbol", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
			state "-2",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_updating_-2_fc.png" // label: 'updating...',	
			state "0",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_sunny_00_fc.png" // label: 'Sunny',			
			state "1",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png" // label: 'Few Clouds',	
			state "2",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_partly_cloudy_02_fc.png"
			state "3",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_mostly_cloudy_03_fc.png"
			state "4",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_cloudy_04_fc.png"
			state "5",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_drizzle_05_fc.png"
			state "6",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_rain_06_fc.png"
			state "7",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png"
			state "8",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_rain_06_fc.png"
			state "9",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png"
			state "10",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_snow_10_fc.png"
			state "11",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_flurries_11_fc.png"
			state "12",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png"
			state "13",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_snow_10_fc.png"
			state "14",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_freezing_rain_07_fc.png"
			state "15",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_thunderstorms_15_fc.png"
			state "16",			icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/weather_windy_16.png"
			state "17",			icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/weather_tornado_17.png"
			state "18",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"
			state "19",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Hazy
			state "20",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Smoke
			state "21",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Dust
            
            // Night Time Icons (Day time Value + 100)
			state "100",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_clear_night_100_fc.png" // label: 'Sunny',			
			state "101",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png" // label: 'Few Clouds',	
			state "102",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_partly_cloudy_101_fc.png"
			state "103",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_mostly_cloudy_103_fc.png"
			state "104",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_cloudy_04_fc.png"
			state "105",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_drizzle_105_fc.png"
			state "106",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png"
			state "107",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png"
			state "108",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_rain_106_fc.png"
			state "109",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png"
			state "110",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons//weather_night_snow_110_fc.png"
			state "111",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_flurries_111_fc.png"
			state "112",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png"
			state "113",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_snow_110_fc.png"
			state "114",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_freezing_rain_107_fc.png"
			state "115",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_night_thunderstorms_115_fc.png"
			state "116",			icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/weather_windy_16.png"
			state "117",			icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/weather_tornado_17.png"
			state "118",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png"
			state "119",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Hazy
			state "120",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Smoke
			state "121",			icon: "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/weather_fog_18_fc.png" // Dust
		}
        standardTile("weatherTemperature", "device.weatherTemperature", width: 2, height: 2, decoration: "flat") {
			state "default", action: "noOp", nextState: "default", label: 'Out: ${currentValue}°', icon: //"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/thermometer.png"
             "https://raw.githubusercontent.com/SANdood/Ecobee/master/icons/thermometer_fc.png"
		}
        
        valueTile("lastPoll", "device.lastPoll", height: 1, width: 5, decoration: "flat") {
			state "thermostatStatus", label:'Last Poll: ${currentValue}', backgroundColor:"#ffffff"
		}
        
		valueTile("holdStatus", "device.holdStatus", height: 1, width: 5, decoration: "flat") {
			state "default", label:'${currentValue}' //, backgroundColor:"#000000", foregroudColor: "#ffffff"
		}
		
        standardTile("ecoLogo", "device.logo", inactiveLabel: false, width: 1, height: 1) {
			state "default",  icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/header_ecobeeicon_blk.png"			
		}

        standardTile("oneBuffer", "device.logo", inactiveLabel: false, width: 1, height: 1, decoration: "flat") {
        	state "default"
        }
        
        valueTile("fanMinOnTime", "device.fanMinOnTime", width: 1, height: 1, decoration: "flat") {
        	state "fanMinOnTime", /*"default",  action: "noOp", nextState: "default", */ label: 'Fan On\n${currentValue}m/hr'
        }
        valueTile("tstatDate", "device.tstatDate", width: 1, height: 1, decoration: "flat") {
        	state "default", /*"default",  action: "noOp", nextState: "default", */ label: '${currentValue}'
        }
        valueTile("tstatTime", "device.tstatTime", width: 1, height: 1, decoration: "flat") {
        	state "default", /*"default",  action: "noOp", nextState: "default", */ label: '${currentValue}'
        }
        standardTile("commandDivider", "device.logo", inactiveLabel: false, width: 4, height: 1, decoration: "flat") {
        	state "default", icon:"https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/command_divider.png"			
        }    
        standardTile("cooling", "device.logo", inactiveLabel: false, width:1, height:1, decoration: "flat") {
        	state "default", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_cool.png"
        }
        standardTile("heating", "device.logo", inactiveLabel: false, width:1, height:1, decoration: "flat") {
        	state "default", icon: "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/png/operatingstate_heat.png"
        }
    
		main(["temperature", "temperatureDisplay"])
		details([
        	// Use this if you are on a fully operational device OS (such as iOS or Android)
        	"temperatureDisplay",
            // Use the lines below if you can't (or don't want to) use the multiAttributeTile version
            // To use, uncomment these lines below, and comment out the line above
            // "temperature", "humidity",  "upButtonControl", "thermostatSetpoint", 
            // "currentStatus", "downButtonControl",
            
        	/* "operatingState", */  "equipmentState", "weatherIcon",  "refresh",  
            "currentProgramIcon", "weatherTemperature", "motionState", 
            "holdStatus", "fanMinOnTime", 
            "tstatDate", "commandDivider", "tstatTime",
            "modeShow", "fanModeLabeled",  "resumeProgram", 
            'cooling',"coolSliderControl", "coolingSetpoint",
            'heating',"heatSliderControl", "heatingSetpoint",            
            "fanMode", "fanModeAutoSlider", "fanModeOnSlider", 
            // "currentProgram", "apiStatus",
            "setHome", "setAway", "setSleep",
            "setModeHeat", "setModeCool", "setModeAuto",
            "apiStatus", "lastPoll"
            // "fanOffButton", "fanCirculate", "setVariable"
            ])            
	}

	preferences {
    	section (title: "${getVersionLabel()}") {
			input "holdType", "enum", title: "Hold Type", description: "When changing temperature, use Temporary or Permanent hold (default)", required: false, options:["Temporary", "Permanent"]
        	// TODO: Add a preference for the background color for "idle"
        	// TODO: Allow for a "smart" Setpoint change in "Auto" mode. Why won't the paragraph show up in the Edit Device screen?
        	// paragraph "The Smart Auto Temp Adjust flag allows for the temperature to be adjusted manually even when the thermostat is in Auto mode. An attempt to determine if the heat or cool setting should be changed will be made automatically."
            input "smartAuto", "bool", title: "Smart Auto Temp Adjust", description: "This flag allows the temperature to be adjusted manually when the thermostat " +
					"is in Auto mode. An attempt to determine if the heat or cool setting should be changed is made automatically.", required: false
            // input "detailedTracing", "bool", title: "Enable Detailed Tracing", description: true, required: false
       }
	}
}

// parse events into attributes
def parse(String description) {
	LOG( "parse() --> Parsing '${description}'" )
	// Not needed for cloud connected devices
}

def refresh() {
	LOG("refresh() called",2,null,'info')
	poll()
}

void poll() {
	LOG("Executing 'poll' using parent SmartApp", 2, null, 'info')
    parent.pollChildren(getDeviceId()) // tell parent to just poll me silently -- can't pass child/this for some reason
}

// Health Check will ping us based on the frequency we configure in Ecobee (Connect) (derived from poll & watchdog frequency)
def ping() {
	LOG("Health Check ping - apiConnected: ${device.currentValue('apiConnected')}, ecobeeConnected: ${device.currentValue('ecobeeConnected')}, checkInterval: ${device.currentValue('checkInterval')} seconds",1,null,'warn')
   	parent.pollChildren(getDeviceId()) 	// forcePoll just me
}

def generateEvent(Map results) {
	LOG("generateEvent(): parsing data ${results}", 4)
    //LOG("Debug level of parent: ${parent.settings.debugLevel}", 4, null, "debug")
	def linkText = getLinkText(device)
    def isMetric = wantMetric()

	def updateTempRanges = false
    def precision = device.currentValue('decimalPrecision')
    if (!precision) precision = (tempScale == 'C') ? 1 : 0
    Integer objectsUpdated = 0
	
	if(results) {
		results.each { name, value ->
			objectsUpdated++
            LOG("generateEvent() - In each loop: object #${objectsUpdated} name: ${name} value: ${value}", 4)
            
            String tempDisplay = ""
			def eventFront = [name: name, linkText: linkText, handlerName: name]
			def event = [:]
            String sendValue = value.toString()
			def isChange = isStateChange(device, name, sendValue)
			
			switch (name) {
				case 'temperature':
				case 'heatingSetpoint':
				case 'coolingSetpoint':
				case 'weatherTemperature':
                case 'thermostatSetpoint':
                    if (isChange) {
                    	//String sendValue = "${value}"		// Already rounded to appropriate user precision (except temperature, which is sent in API precision)
                    	event = eventFront + [value: sendValue,  descriptionText: getTemperatureDescriptionText(name, value, linkText), isStateChange: true, displayed: true]
						if (name=="temperature") {
							// Generate the display value that will preserve decimal positions ending in 0
                    		if (precision == 0) {
                    			tempDisplay = value.toDouble().round(0).toInteger().toString() + '°'
                    		} else {
								tempDisplay = String.format( "%.${precision.toInteger()}f", value.toDouble().round(precision.toInteger())) + '°'
                    		}
						} 
                    }
					break;
				
				case 'thermostatOperatingState':
                	// A little tricky, but this is how we display Smart Recovery within the stock Thermostat multiAttributeTile
                    // thermostatOperatingStateDisplay has the same values as thermostatOperatingState, plus "heating (smart recovery)" 
                    // and "cooling (smart recovery)". We separately update the standard thermostatOperatingState attribute.
                    isChange = isStateChange(device, "${name}Display", sendValue) 
                    boolean displayDesc
                    String descText
                    String realValue
                    if (sendValue.contains('(')) {
                    	displayDesc = true				// only show this update ThermOpStateDisplay if we are in Smart Recovery
                    	descText = 'in Smart Recovery'	// equipmentOperatingState will show what is actually running
                        realValue = sendValue.take(7)	// this gets us to back to just heating/cooling
                    } else {
                    	displayDesc = false				// hide this message - is redundant with EquipOpState
                        descText = sendValue.capitalize()
                        realValue = sendValue
                    }
                   	if (isChange) {
                    	sendEvent(name: "${name}Display", value: sendValue, descriptionText: "Thermostat is ${descText}", linkText: linkText, 
                    					handlerName: "${name}Display", isStateChange: true, displayed: displayDesc)
                        objectsUpdated++
                    }
                    
                    // now update thermostatOperatingState - is limited by API to idle, fan only, heating, cooling, pending heat, pending cool
                    isChange = isStateChange(device, name, realValue)
					if (isChange) event = eventFront + [value: realValue, descriptionText: "Thermostat is ${realValue}", isStateChange: true, displayed: false]
                	break;
				
				case 'equipmentOperatingState':
					if (isChange) event = eventFront + [value: "${value}", descriptionText: "Equipment is ${value}", isStateChange: true, displayed: true]
					break;
				
				case 'equipmentStatus':
					if (isChange) {
                    	String descText = (value == 'idle') ? 'Equipment is idle' : ((value == 'offline') ? 'Equipment is offline' : "Equipment is running ${value}")
                    	event = eventFront +  [value: "${value}", descriptionText: descText, isStateChange: true, displayed: false]
                    }
					break;
				
           		case 'lastPoll':
					if (isChange) event = eventFront + [value: "${value}", descriptionText: "Poll: ${value}", isStateChange: true, displayed: true]
					break;
				
				case 'humidity':
                	def humSetpoint = device.currentValue('humiditySetpoint') 
                    if (humSetpoint == null) humSetpoint = 0
                    String setpointText = (humSetpoint == 0) ? '' : " (setpoint: ${humSetpoint}%)"
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "Humidity is ${sendValue}%${setpointText}", isStateChange: true, displayed: true]
            		break;
				
				case 'humiditySetpoint':
					if (isChange && (value.toInteger() != 0)) {
                    	event = eventFront + [value: sendValue, descriptionText: "Humidity setpoint is ${sendValue}%", isStateChange: true, displayed: false]
                        def hum = device.currentValue('humidity')
                        if (hum == null) hum = 0
		            	sendEvent( name: 'humidity', linkText: linkText, handlerName: 'humidity', descriptionText: "Humidity is ${hum}% (setpoint: ${sendValue}%)", isStateChange: false, displayed: true )
                        objectsUpdated++
                    }
                    break;
				
				case 'currentProgramName':
                	String progText = ''
                    if (sendValue == 'Vacation') {
                    	progText = 'Program is Vacation'
                        sendEvent(name: 'resumeProgram', value: 'cancel', displayed: false)	// change the button to Cancel Vacation
                    } else if (sendValue == 'Offline') {
                    	progText = 'Thermostat is Offline'
                    } else {
                    	progText = 'Program is '+sendValue.trim().replaceAll(':','')
                        // TODO: Should really disable the button if we aren't in Hold: * or Auto: * programs
                        sendEvent(name: 'resumeProgram', value: 'resume', displayed: false)	// change the button to Resume Program
                    }
					if (isChange) event = eventFront + [value: sendValue, descriptionText: progText, isStateChange: true, displayed: true]
					break;
				
				case 'apiConnected':
                	if (isChange) event = eventFront + [value: sendValue, descriptionText: "API Connection is ${value}", isStateChange: true, displayed: true]
					break;
				
				case 'weatherSymbol':
                case 'timeOfDay':
					// Check to see if it is night time, if so change to a night symbol
                    Integer symbolNum = (name == 'weatherSymbol') ? value.toInteger() : device.currentValue('weatherSymbol').toInteger()
                    String timeOfDay
                    if (name == 'timeOfDay') {
                    	timeOfDay = value
                        objectsUpdated++
                    } else {
                    	timeOfDay = device.currentValue('timeOfDay')
                    }
                    if ((timeOfDay == 'night') && (symbolNum < 100)) { 
                    	symbolNum = symbolNum + 100
                    } else if ((timeOfDay == 'day') && (symbolNum >= 100)) {
                    	symbolNum = symbolNum - 100
					}
					isChange = isStateChange(device, 'weatherSymbol', symbolNum.toString())
					if (isChange) event = [name: 'weatherSymbol', linkText: linkText, handlerName: 'weatherSymbol', value: "${symbolNum}", descriptionText: "Weather Symbol is ${symbolNum}", isStateChange: true, displayed: true]
					sendEvent( name: 'timeOfDay', value: timeOfDay, displayed: false )
                    break;
				
				case 'thermostatHold':
					if (isChange) {
                    	String descText = (sendValue == '') ? 'Hold finished' : (value == 'hold') ? "Hold ${device.currentValue('currentProgram')} (${device.currentValue('scheduledProgram')})" : "Hold for ${sendValue}"
                    	event = eventFront + [value: sendValue, descriptionText: descText, isStateChange: true, displayed: true]
                    }
					break;
				
				case 'holdStatus': 
					if (isChange) event = eventFront + [value: sendValue, descriptionText: sendValue, isStateChange: true, displayed: (value != '')]
					break;
                    
              	case 'motion': 
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "Motion is ${sendValue}", isStateChange: true, displayed: true]
					break;
				
				case 'fanMinOnTime':
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "Fan On ${sendValue} minutes per hour", isStateChange: true, displayed: true]
					break;
				
				case 'thermostatMode':
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "Mode is ${sendValue}", isStateChange: true, displayed: true]
		            break;
				
        		case 'thermostatFanMode':
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "Fan Mode is ${sendValue}", isStateChange: true, displayed: true]
            		break;
				
				case 'debugEventFromParent':
					event = eventFront + [value: sendValue, descriptionText: "-> ${sendValue}", isStateChange: true, displayed: true]
					break;
                    
                case 'thermostatTime':
                // 2017-03-22 15:06:14
                	String tstatDate = sendValue.take(4) + '\n' + sendValue.drop(5).take(5)
                    String tstatTime = sendValue.drop(11).take(5)
                    int hours = tstatTime.take(2).toInteger()
                    int mins = tstatTime.drop(3).toInteger()
                    if (hours < 12) {
                    	if (hours==0) hours = 12
                    	tstatTime = "${hours}" + tstatTime.drop(2) + 'am'
                    } else {
                    	if (hours==12) hours = 24
                        tstatTime = "${hours-12}" + tstatTime.drop(2) + "pm"
                    }
                    if (isStateChange(device, 'tstatDate', tstatDate)) sendEvent(name: 'tstatDate', value: tstatDate, isStateChange: true, displayed: false)
                    if (isStateChange(device, 'tstatTime', tstatTime)) sendEvent(name: 'tstatTime', value: tstatTime, isStateChange: true, displayed: false)
                    if (isChange) event = eventFront + [value: sendValue, isStateChange: true, displayed: false]
                    objectsUpdated++
                    break;
                    
				
				// These are ones we don't need to display or provide descriptionText for (mostly internal or debug use)
				case 'debugLevel':
				case 'heatRangeLow':
				case 'heatRangeHigh':
				case 'coolRangeLow':
				case 'coolRangeHigh':
				case 'heatRange':
				case 'coolRange':
				case 'decimalPrecision':
				// case 'timeOfDay':
				case 'heatMode':
				case 'coolMode':
				case 'autoMode':
				case 'auxHeatMode':
				case 'currentProgramId':
				case 'currentProgram':
				case 'scheduledProgramName':
				case 'scheduledProgramId':
				case 'scheduledProgram':
				case 'heatStages':
				case 'coolStages':
				case 'hasHeatPump':
				case 'hasForcedAir':
				case 'hasElectric':
				case 'hasBoiler':
				case 'auxHeatMode':
				case 'heatDifferential':
				case 'coolDifferential':
                case 'programsList':
                case 'holdEndsAt':
                case 'temperatureScale':
                case 'checkInterval':
                case 'ecobeeConnected':
					if (isChange) event = eventFront +  [value: sendValue, isStateChange: true, displayed: false]
					break;
				
				// everything else just gets displayed with generic text
				default:
					if (isChange) event = eventFront + [value: sendValue, descriptionText: "${name} is ${sendValue}", isStateChange: true, displayed: true]			
					break;
			}
			if (event != [:]) {
				LOG("generateEvent() - Out of switch{}, calling sendevent(${event})", 5)
				sendEvent(event)
			}
            if (tempDisplay != "") {
        		event = [ name: 'temperatureDisplay', value: tempDisplay, linkText: linkText, descriptionText:"Temperature Display is ${tempDisplay}", displayed: false ]
        		sendEvent(event)
            	LOG("generateEvent() - Temperature updated, calling sendevent(${event})", 5)
                objectsUpdated++
        	}
		}
		generateSetpointEvent()
		generateStatusEvent()
	}
    LOG("Updated ${objectsUpdated} object${objectsUpdated!=1?'s':''}",2,this,'info')
}

//return descriptionText to be shown on mobile activity feed
private getTemperatureDescriptionText(name, value, linkText) {
	switch (name) {
		case 'temperature':
			return "Temperature is ${value}°"
            break;
		case 'heatingSetpoint':
			return "Heating setpoint is ${value}°"
            break;
        case 'coolingSetpoint':
			return "Cooling setpoint is ${value}°"
            break;
        case 'thermostatSetpoint':
        	return "Thermostat setpoint is ${value}°"
            break;
        case 'weatherTemperature':
        	return "Outside temperature is ${value}°"
            break;
	}
}

// Does not set in absolute values, sets in increments either up or down
def setTemperature(setpoint) {
	LOG("setTemperature() called with setpoint ${setpoint}. Current temperature: ${device.currentValue("temperature")}. Heat Setpoint: ${device.currentValue("heatingSetpoint")}. Cool Setpoint: ${device.currentValue("coolingSetpoint")}. Thermo Setpoint: ${device.currentValue("thermostatSetpoint")}", 4)

    def mode = device.currentValue("thermostatMode")
    def midpoint
	def targetvalue

	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("setTemperature(): this mode: $mode does not allow raiseSetpoint", 2, null, "warn")
        return
    }

	def currentTemp = device.currentValue("temperature")
    def deltaTemp = 0

	if (setpoint == 0) { // down arrow pressed
    	deltaTemp = -1
    } else if (setpoint == 1) { // up arrow pressed
    	deltaTemp = 1
    } else {
    	deltaTemp = ( (setpoint - currentTemp) < 0) ? -1 : 1
    }
    
    LOG("deltaTemp = ${deltaTemp}")

    if (mode == "auto") {
    	// In Smart Auto Mode
		LOG("setTemperature(): In Smart Auto Mode", 4)

        if (deltaTemp < 0) {
        	// Decrement the temp for cooling
            LOG("Smart Auto: lowerSetpoint being called", 4)
            lowerSetpoint()
        } else if (deltaTemp > 0) {
        	// Increment the temp for heating
            LOG("Smart Auto: raiseSetpoint being called", 4)
            raiseSetpoint()
        } // Otherwise they are equal and the setpoint does not change

    } else if (mode == "heat") {
    	// Change the heat
        LOG("setTemperature(): change the heat temp", 4)
        // setHeatingSetpoint(setpoint)
        if (deltaTemp < 0) {
        	// Decrement the temp for cooling
            LOG("Heat: lowerSetpoint being called", 4)
            lowerSetpoint()
        } else if (deltaTemp > 0) {
        	// Increment the temp for heating
            LOG("Heat: raiseSetpoint being called", 4)
            raiseSetpoint()
        } // Otherwise they are equal and the setpoint does not change

    } else if (mode == "cool") {
    	// Change the cool
        LOG("setTemperature(): change the cool temp", 4)
        // setCoolingSetpoint(setpoint)
        if (deltaTemp < 0) {
        	// Decrement the temp for cooling
            LOG("Cool: lowerSetpoint being called", 4)
            lowerSetpoint()
        } else if (deltaTemp > 0) {
        	// Increment the temp for heating
            LOG("Cool: raiseSetpoint being called", 4)
            raiseSetpoint()
        } // Otherwise they are equal and the setpoint does not change

    }
}

void setHeatingSetpoint(setpoint) {
	LOG("setHeatingSetpoint() request with setpoint value = ${setpoint} before toDouble()", 4)
	setHeatingSetpoint(setpoint.toDouble())
}

void setHeatingSetpoint(Double setpoint) {
//    def mode = device.currentValue("thermostatMode")
	LOG("setHeatingSetpoint() request with setpoint value = ${setpoint}", 4)

	def heatingSetpoint = setpoint
	def coolingSetpoint = device.currentValue("coolingSetpoint").toDouble()
	def deviceId = getDeviceId()

	LOG("setHeatingSetpoint() before compare: heatingSetpoint == ${heatingSetpoint}   coolingSetpoint == ${coolingSetpoint}", 4)
	//enforce limits of heatingSetpoint vs coolingSetpoint
	def low = device.currentValue("heatRangeLow")
	def high = device.currentValue("heatRangeHigh")
	
	if (heatingSetpoint < low ) { heatingSetpoint = low }
	if (heatingSetpoint > high) { heatingSetpoint = high}
	if (heatingSetpoint > coolingSetpoint) {
		coolingSetpoint = heatingSetpoint
	}

	LOG("Sending setHeatingSetpoint> coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}")

	def sendHoldType = whatHoldType()

	if (parent.setHold(this, heatingSetpoint,  coolingSetpoint, deviceId, sendHoldType)) {
		sendEvent("name":"heatingSetpoint", "value": wantMetric() ? heatingSetpoint : heatingSetpoint.toDouble().round(0).toInteger() )
		sendEvent("name":"coolingSetpoint", "value": wantMetric() ? coolingSetpoint : coolingSetpoint.toDouble().round(0).toInteger() )
		LOG("Done setHeatingSetpoint> coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}")
		generateSetpointEvent()
		generateStatusEvent()
	} else {
		LOG("Error setHeatingSetpoint(${setpoint})", 1, null, "error") //This error is handled by the connect app
        
	}
}

void setCoolingSetpoint(setpoint) {
	LOG("setCoolingSetpoint() request with setpoint value = ${setpoint} (before toDouble)", 4)

	setCoolingSetpoint(setpoint.toDouble())
}

void setCoolingSetpoint(Double setpoint) {
	LOG("setCoolingSetpoint() request with setpoint value = ${setpoint}", 4)
//    def mode = device.currentValue("thermostatMode")
	def heatingSetpoint = device.currentValue("heatingSetpoint").toDouble()
	def coolingSetpoint = setpoint
	def deviceId = getDeviceId()


	LOG("setCoolingSetpoint() before compare: heatingSetpoint == ${heatingSetpoint}   coolingSetpoint == ${coolingSetpoint}")

	//enforce limits of heatingSetpoint vs coolingSetpoint
	def low = device.currentValue("coolRangeLow")
	def high = device.currentValue("coolRangeHigh")
	
	if (coolingSetpoint < low ) { coolingSetpoint = low }
	if (coolingSetpoint > high) { coolingSetpoint = high}
	if (heatingSetpoint > coolingSetpoint) {
		heatingSetpoint = coolingSetpoint
	}

	LOG("Sending setCoolingSetpoint> coolingSetpoint: ${coolingSetpoint}, heatingSetpoint: ${heatingSetpoint}")
	def sendHoldType = whatHoldType()
    LOG("sendHoldType == ${sendHoldType}", 5)

    // Convert temp to F from C if needed
	if (parent.setHold(this, heatingSetpoint,  coolingSetpoint, deviceId, sendHoldType)) {
		sendEvent("name":"heatingSetpoint", "value": wantMetric() ? heatingSetpoint : heatingSetpoint.toDouble().round(0).toInteger() )
		sendEvent("name":"coolingSetpoint", "value": wantMetric() ? coolingSetpoint : coolingSetpoint.toDouble().round(0).toInteger() )
		LOG("Done setCoolingSetpoint>> coolingSetpoint = ${coolingSetpoint}, heatingSetpoint = ${heatingSetpoint}", 4)
		generateSetpointEvent()
		generateStatusEvent()
	} else {
		LOG("Error setCoolingSetpoint(setpoint)", 2, null, "error") //This error is handled by the connect app
	}
}

void resumeProgram(resumeAll=true) {
	resumeProgramInternal(resumeAll)
}

def resumeProgramInternal(resumeAll=true) {
	def result = true
    
	String thermostatHold = device.currentValue('thermostatHold')
	if (thermostatHold == '') {
		LOG('resumeProgram() - No current hold', 2, null, 'info')
        sendEvent(name: 'resumeProgram', value: 'resume', descriptionText: 'resumeProgram is done', displayed: false, isStateChange: true)
		return result
	} else if (thermostatHold =='vacation') { // this shouldn't happen anymore - button changes to Cancel when in Vacation mode
		LOG('resumeProgram() - Cannot resume from Vacation hold', 2, null, 'error')
        sendEvent(name: 'resumeProgram', value: 'resume', descriptionText: 'resumeProgram is done', displayed: false, isStateChange: true)
		return false
	} else {
		LOG("resumeProgram() - Hold type is ${thermostatHold}", 4)
	}
	
	sendEvent(name: 'thermostatStatus', value: 'Resuming scheduled Program...', displayed: false)
	def deviceId = getDeviceId()
	if (parent.resumeProgram(this, deviceId, resumeAll)) {
		sendEvent(name: 'thermostatStatus', value: 'Program updating...', displayed: false)
        if (resumeAll) generateProgramEvent(device.currentValue('scheduledProgramName'))
        sendEvent(name: "resumeProgram", value: "resume", descriptionText: "resumeProgram is done", displayed: false, isStateChange: true)
        sendEvent(name: "holdStatus", value: '', descriptionText: 'Hold finished', displayed: true, isStateChange: true)
        LOG("resumeProgram(${resumeAll}) - succeeded", 2,null,'info')
	} else {
		sendEvent(name: "thermostatStatus", value: "Resume Program failed..", description:statusText, displayed: false)
		LOG("resumeProgram() - failed (parent.resumeProgram(this, ${deviceId}, ${resumeAll}))", 1, null, "error")
        result = false
	}
    runIn(5, poll, [overwrite: true])
    return result
}

void cancelVacation() {
	// Cancel the current vacation...
    deleteVacation(null)
}

/*
def fanModes() {
	["off", "on", "auto", "circulate"]
}
*/

//def generateQuickEvent(name, value) {
//	generateQuickEvent(name, value, 0)
//}

def generateQuickEvent(String name, String value, Integer pollIn=0) {
	sendEvent(name: name, value: value, displayed: false)
    if (pollIn > 0) { runIn(pollIn, 'poll', [overwrite: true]) }
}

void setThermostatMode(String value) {
	// 	"emergencyHeat" "heat" "cool" "off" "auto"
    
    if (value=='emergency' || value=='emergencyHeat') { value = 'auxHeatOnly' }    
	LOG("setThermostatMode(${value})", 5)	

    def deviceId = getDeviceId()
	if (parent.setMode(this, value, deviceId)) {
    	generateQuickEvent('thermostatMode', value, 5)
		LOG("Success changing thermostat mode to ${value}",2,null,'info')
	} else {
		LOG("Failed to set thermostat mode to ${value}.", 1, null, 'error')
	}
}

void off() {
	LOG("off()", 5)
    setThermostatMode("off")    
}

void heat() {
	LOG("heat()", 5)
    setThermostatMode("heat")    
}

void auxHeatOnly() {
	LOG("auxHeatOnly()", 5)
    setThermostatMode("auxHeatOnly")
}

void emergency() {
	LOG("emergency()", 5)
    setThermostatMode("auxHeatOnly")
}

// This is the proper definition for the capability
void emergencyHeat() {
	LOG("emergencyHeat()", 5)
    setThermostatMode("auxHeatOnly")
}

void cool() {
	LOG("cool()", 5)
    setThermostatMode("cool")    
}

void auto() {
	LOG("auto()", 5)
    setThermostatMode("auto")    
}

// Handle Comfort Settings
void setThermostatProgram(String program, String holdType='') {
	// Change the Comfort Setting (aka Climate)
    def programsList = []
    programsList = new JsonSlurper().parseText(device.currentValue('programsList'))
    if (!programsList.contains(program)) {
    	LOG("setThermostatProgram(${program}) - invalid argument",2,this,'warn')
        return
    }
    LOG("setThermostatProgram() program: ${program} holdType: ${holdType}", 4)
	def deviceId = getDeviceId()    

	LOG("Before calling parent.setProgram()", 5)
	
    def sendHoldType = (holdType!='') ? holdType : whatHoldType()
    poll()		// need to know if scheduled program changed recently
	
	// if the requested program is the same as the one that is supposed to be running, then just resumeProgram
	if (device.currentValue("scheduledProgram") == program) {
		LOG("Resuming scheduled program ${program}", 2, this, 'info')
		if (resumeProgramInternal(true)) {							// resumeAll so that we get back to scheduled program
			if (sendHoldType == 'nextTransition') {		// didn't want a permanent hold, so we are all done
           		generateProgramEvent(program,'')
            	runIn(5, poll, [overwrite: true])
           		return
        	}
        }
	} else {
       	resumeProgram(true)							// resumeAll before we change the program
    }
  
    if ( parent.setProgram(this, program, deviceId, sendHoldType) ) {
    	LOG("Success setting Program to Hold: ${program}",2,this,'info')
		generateProgramEvent('Hold: '+program)
	} else {
    	LOG("Error setting Program to ${program}", 2, this, "warn")
		// def priorProgram = device.currentState("currentProgramId")?.value
		// generateProgramEvent(priorProgram, program) // reset the tile back
	}
 	runIn(5, poll, [overwrite: true])
    
 	LOG("After calling parent.setProgram()", 5)  
}

void home() {
	// Change the Comfort Setting to Home
    LOG("home()", 5)
    setThermostatProgram("Home")
}

void present(){
	// Change the Comfort Setting to Home (Nest compatibility)
    LOG("present()", 5)
    setThermostatProgram("Home")
}
void away() {
	// Change the Comfort Setting to Away
    LOG("away()", 5)
    setThermostatProgram("Away")
}

// Unfortunately, we can't overload the internal Java/Groovy/system definition of 'sleep()'
/*def sleep() {
	// Change the Comfort Setting to Sleep    
    LOG("sleep()", 5)
    setThermostatProgram("Sleep")
}
*/
void asleep() {
	// Change the Comfort Setting to Sleep    
    LOG("asleep()", 5)
    setThermostatProgram("Sleep")
}

void night() {
	// Change the Comfort Setting to Sleep    
    LOG("night()", 5)
    setThermostatProgram("Sleep")
}

def generateProgramEvent(String program, String failedProgram='') {
	LOG("Generate generateProgramEvent Event: program ${program}", 4)
    
	sendEvent(name: "thermostatStatus", value: "Setpoint updating...", /* description: statusText, */ displayed: false)

	String prog = program.capitalize()
    String progHold = (failedProgram == '') ? prog : "Hold: "+prog
    def updates = ['currentProgramName':progHold,'currentProgramId':program.toLowerCase(),'currentProgram': prog]
    generateEvent(updates)
    
    def tileName = ""
    
    if (!failedProgram) {
    	tileName = "set" + program.capitalize()    	
    } else {
    	tileName = "set" + failedProgram.capitalize()    	
    }
    sendEvent(name: "${tileName}", value: "${program}", descriptionText: "${tileName} is done", displayed: false, isStateChange: true)
}

def setThermostatFanMode(value, holdType=null) {
	LOG("setThermostatFanMode(${value})", 4)
	// "auto" "on" "circulate" "off"       
    
    // This is to work around a bug in some SmartApps that are using fanOn and fanAuto as inputs here, which is wrong
    if (value == "fanOn" || value == "on" ) { value = "on" }
    else if (value == "fanAuto" || value == "auto" ) { value = "auto" }
    else if (value == "fanCirculate" || value == "circulate")  { value == "circulate" }
    else if (value == "fanOff" || value == "off") { value = "off" }
	else {
    	LOG("setThermostatFanMode() - Unrecognized Fan Mode: ${value}. Setting to 'auto'", 1, null, "error")
        value = "auto"
    }
    
    // Change the state now to quickly refresh the UI
    generateQuickEvent("thermostatFanMode", value, 0)
    
    def results = parent.setFanMode(this, value, getDeviceId())
    
	if ( results ) {
    	LOG("setFanMode(${value}) completed successfully", 2,this,'info')
        runIn(5, poll, [overwrite: true])
    } else {
    	generateQuickEvent("thermostatFanMode", device.currentValue("thermostatFanMode"))
        LOG("setFanMode(${value}) failed", 2,this,'warn')
    }
    
	generateSetpointEvent()
	generateStatusEvent()    
}

def fanOn() {
	LOG("fanOn()", 5)
    setThermostatFanMode("on")
}

def fanAuto() {
	LOG("fanAuto()", 5)
	setThermostatFanMode("auto")
}

def fanCirculate() {
	LOG("fanCirculate()", 5)
    setThermostatFanMode("circulate")
}

def fanOff() {
	LOG("fanOff()", 5)
	setThermostatFanMode("off")
}

void setFanMinOnTime(minutes) {
	LOG("setFanMinOnTime(${minutes})", 5, null, "trace")
    def deviceId = getDeviceId()
    
	def howLong = 10	// default to 10 minutes, if no value supplied
	if (minutes.isNumber()) howLong = minutes
    if ((howLong >=0) && (howLong <=  55)) {
		parent.setFanMinOnTime(this, deviceId, howLong)
        runIn(5, poll, [overwrite: true])
    } else {
    	LOG("setFanMinOnTime(${minutes}) - invalid argument",5,null, "error")
    }
}

void setVacationFanMinOnTime(minutes) {
	LOG("setVacationFanMinOnTime(${minutes})", 5, null, "trace")
    def deviceId = getDeviceId()
    
	def howLong = 0		// default to 0 minutes during Vacations, if no value supplied
	if (minutes.isNumber()) howLong = minutes
    if ((howLong >=0) && (howLong <=  55)) {
		parent.setVacationFanMinOnTime(this, deviceId, howLong)
        runIn(5, poll, [overwrite: true])
    } else {
    	LOG("setVacationFanMinOnTime(${minutes}) - invalid argument",5,null, "error")
    }
}

void deleteVacation(vacationName = null) {
	LOG("deleteVacation(${vacationName})", 5, null, "trace")
    def deviceId = getDeviceId()
    parent.deleteVacation(this, deviceId, vacationName)
    runIn(5, poll, [overwrite: true])
}

def generateSetpointEvent() {
	LOG("Generate SetPoint Event", 5, null, "trace")

	def mode = device.currentValue("thermostatMode")    
    def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = device.currentValue("coolingSetpoint")
    
    if (debugLevel(4)) {
		LOG("Current Mode = ${mode}", 4, null, "debug")
		LOG("Heating Setpoint = ${heatingSetpoint}", 4, null, "debug")
		LOG("Cooling Setpoint = ${coolingSetpoint}", 4, null, "debug")
    }

	switch (mode) {
		case 'heat':
		case 'emergencyHeat':
			sendEvent(name:'thermostatSetpoint', value: "${heatingSetpoint}", displayed: false)
			break;
		
		case 'cool':
			sendEvent(name:'thermostatSetpoint', value: "${coolingSetpoint}", displayed: false)
			break;
		
		case 'auto':
			if (!usingSmartAuto()) {
				// No Smart Auto, just regular auto
				sendEvent(name:'thermostatSetpoint', value:"Auto (${heatingSetpoint}-${coolingSetpoint})", displayed: false)
			} else {
		    	// Smart Auto Enabled
				sendEvent(name:'thermostatSetpoint', value: "${device.currentValue('temperature')}", displayed: false)
			}
			break;
		
		case 'off':
			sendEvent(name:'thermostatSetpoint', value:'Off', displayed: false)
			break;
	}
}

void raiseSetpoint() {
	def mode = device.currentValue("thermostatMode")
	def targetvalue

	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("raiseSetpoint(): this mode: $mode does not allow raiseSetpoint")
        return
	}

   	def heatingSetpoint = device.currentValue("heatingSetpoint").toDouble()
	def coolingSetpoint = device.currentValue("coolingSetpoint").toDouble()
    def thermostatSetpoint = device.currentValue("thermostatSetpoint").toDouble()
    if (device.currentValue("thermostatOpertaingState") == 'idle') {
    	if (thermostatSetpoint == heatingSetpoint) {
        	heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble() 	// correct from the display value
            thermostatSetpoint = heatingSetpoint
            coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
        } else if (thermostatSetpoint == coolingSetpoint) {
         	coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
            thermostatSetpoint = coolingSetpoint
            heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble()
        } else {
          	heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble()
            coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
        }
    }
	
	LOG("raiseSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}", 4)

   	if (thermostatSetpoint) {
		targetvalue = thermostatSetpoint
	} else {
		targetvalue = 0.0
	}

       if (getTemperatureScale() == "C" ) {
       	targetvalue = targetvalue.toDouble() + 0.5
       } else {
		targetvalue = targetvalue.toDouble() + 1.0
       }

	sendEvent(name: "thermostatSetpoint", value: "${( wantMetric() ? targetvalue : targetvalue.round(0).toInteger() )}", displayed: false)
	LOG("In mode $mode raiseSetpoint() to $targetvalue", 4)

	def runWhen = parent.settings?.arrowPause ?: 4		
	runIn(runWhen, "alterSetpoint", [data: [value:targetvalue], overwrite: true]) //when user click button this runIn will be overwrite
}

//called by tile when user hit raise temperature button on UI
void lowerSetpoint() {
	def mode = device.currentValue("thermostatMode")
	def targetvalue

	if (mode == "off" || (mode == "auto" && !usingSmartAuto() )) {
		LOG("lowerSetpoint(): this mode: $mode does not allow lowerSetpoint", 2, null, "warn")
    } else {
    	def heatingSetpoint = device.currentValue("heatingSetpoint")
		def coolingSetpoint = device.currentValue("coolingSetpoint")
		def thermostatSetpoint = device.currentValue("thermostatSetpoint").toDouble()
    	if (device.currentValue("thermostatOpertaingState") == 'idle') {
    		if (thermostatSetpoint == heatingSetpoint) {
        		heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble() 	// correct from the display value
            	thermostatSetpoint = heatingSetpoint
            	coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
        	} else if (thermostatSetpoint == coolingSetpoint) {
         		coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
            	thermostatSetpoint = coolingSetpoint
            	heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble()
        	} else {
          		heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble()
            	coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
        	}	
    	}
		LOG("lowerSetpoint() mode = ${mode}, heatingSetpoint: ${heatingSetpoint}, coolingSetpoint:${coolingSetpoint}, thermostatSetpoint:${thermostatSetpoint}", 4)

        if (thermostatSetpoint) {
			targetvalue = thermostatSetpoint
		} else {
			targetvalue = 0.0
		}

        if (getTemperatureScale() == "C" ) {
        	targetvalue = targetvalue.toDouble() - 0.5
        } else {
			targetvalue = targetvalue.toDouble() - 1.0
        }

		sendEvent(name: "thermostatSetpoint", value: "${( wantMetric() ? targetvalue : targetvalue.round(0).toInteger() )}", displayed: false)
		LOG("In mode $mode lowerSetpoint() to $targetvalue", 5, null, "info")

		// Wait 4 seconds before sending in case we hit the buttons again
	def runWhen = parent.settings?.arrowPause ?: 4		
	runIn(runWhen, "alterSetpoint", [data: [value:targetvalue], overwrite: true]) //when user click button this runIn will be overwrite
	}
}

//called by raiseSetpoint() and lowerSetpoint()
void alterSetpoint(temp) {
	def mode = device.currentValue("thermostatMode")
	def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = device.currentValue("coolingSetpoint")
    def thermostatSetpoint = device.currentValue("thermostatSetpoint")
    if (device.currentValue("thermostatOpertaingState") == 'idle') {
    	if (thermostatSetpoint == heatingSetpoint) {
        	heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble() 	// correct from the display value
            thermostatSetpoint = heatingSetpoint
            coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
        } else if (thermostatSetpoint == coolingSetpoint) {
         	coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
            thermostatSetpoint = coolingSetpoint
            heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble()
        } else {
          	heatingSetpoint = heatingSetpoint + device.currentValue("heatDifferential").toDouble()
            coolingSetpoint = coolingSetpoint - device.currentValue("coolDifferential").toDouble()
        }
    }
    def currentTemp = device.currentValue("temperature")
    def heatHigh = device.currentValue('heatHigh')
    def heatLow = device.currentValue('heatLow')
    def coolHigh = device.currentValue('coolHigh')
    def coolLow = device.currentValue('coolLow')
    def saveThermostatSetpoint = thermostatSetpoint
	def deviceId = getDeviceId()

	def targetHeatingSetpoint = heatingSetpoint
	def targetCoolingSetpoint = coolingSetpoing

	LOG("alterSetpoint - temp.value is ${temp.value}", 4)

	//step1: check thermostatMode
	if (mode == "heat"){
    	if (temp.value > heatHigh) targetHeatingSetpoint = heatHigh
        if (temp.value < heatLow) targetHeatingSetpoint = heatLow
		if (temp.value > coolingSetpoint){
			targetHeatingSetpoint = temp.value
			targetCoolingSetpoint = temp.value
		} else {
			targetHeatingSetpoint = temp.value
			targetCoolingSetpoint = coolingSetpoint
		}
	} else if (mode == "cool") {
		//enforce limits before sending request to cloud
    	if (temp.value > coolHigh) targetHeatingSetpoint = coolHigh
        if (temp.value < coolLow) targetHeatingSetpoint = coolLow
		if (temp.value < heatingSetpoint){
			targetHeatingSetpoint = temp.value
			targetCoolingSetpoint = temp.value
		} else {
			targetHeatingSetpoint = heatingSetpoint
			targetCoolingSetpoint = temp.value
		}
	} else if (mode == "auto" && usingSmartAuto() ) {
    	// Make changes based on our Smart Auto mode
        if (temp.value > currentTemp) {
        	// Change the heat settings to the new setpoint
            if (temp.value > heatHigh) targetHeatingSetpoint = heatHigh
        	if (temp.value < heatLow) targetHeatingSetpoint = heatLow
            LOG("alterSetpoint() - Smart Auto setting setpoint: ${temp.value}. Updating heat target")
            targetHeatingSetpoint = temp.value
            targetCoolingSetpoint = (temp.value > coolingSetpoint) ? temp.value : coolingSetpoint
		} else {
        	// Change the cool settings to the new setpoint
            if (temp.value > coolHigh) targetHeatingSetpoint = coolHigh
        	if (temp.value < coolLow) targetHeatingSetpoint = coolLow
			LOG("alterSetpoint() - Smart Auto setting setpoint: ${temp.value}. Updating cool target")
            targetCoolingSetpoint = temp.value

            LOG("targetHeatingSetpoint before ${targetHeatingSetpoint}")
            targetHeatingSetpoint = (temp.value < heatingSetpoint) ? temp.value : heatingSetpoint
            LOG("targetHeatingSetpoint after ${targetHeatingSetpoint}")
        }
    } else {
    	LOG("alterSetpoint() called with unsupported mode: ${mode}", 2, null, "warn")
        // return without changing settings on thermostat
        return
    }

	LOG("alterSetpoint >> in mode ${mode} trying to change heatingSetpoint to ${targetHeatingSetpoint} " +
			"coolingSetpoint to ${targetCoolingSetpoint} with holdType : ${whatHoldType()}")

	def sendHoldType = whatHoldType()
	//step2: call parent.setHold to send http request to 3rd party cloud    
	if (parent.setHold(this, targetHeatingSetpoint, targetCoolingSetpoint, deviceId, sendHoldType)) {
		sendEvent(name: "thermostatSetpoint", value: temp.value.toString(), displayed: false)
		sendEvent(name: "heatingSetpoint", value: targetHeatingSetpoint, displayed: false)
		sendEvent(name: "coolingSetpoint", value: targetCoolingSetpoint, displayed: false)
		LOG("alterSetpoint in mode $mode succeed change setpoint to= ${temp.value}", 4)
	} else {
		LOG("WARN: alterSetpoint() - setHold failed. Could be an intermittent problem.", 1, null, "error")
        sendEvent(name: "thermostatSetpoint", value: saveThermostatSetpoint.toString(), displayed: false)
	}
    // generateSetpointEvent()
	generateStatusEvent()
    // refresh data
    runIn(5, poll, [overwrite: true])
}

// This just updates the generic multiAttributeTile - text should match the Thermostat mAT
def generateStatusEvent() {
	def mode = device.currentValue("thermostatMode")
	def heatingSetpoint = device.currentValue("heatingSetpoint")
	def coolingSetpoint = device.currentValue("coolingSetpoint")
	def temperature = device.currentValue("temperature")
    def operatingState = device.currentValue("thermostatOperatingState")

	def statusText	
    if (debugLevel(4)) {
		LOG("Generate Status Event for Mode = ${mode}", 4)
		LOG("Temperature = ${temperature}", 4)
		LOG("Heating setpoint = ${heatingSetpoint}", 4)
		LOG("Cooling setpoint = ${coolingSetpoint}", 4)
		LOG("HVAC Mode = ${mode}", 4)	
    	LOG("Operating State = ${operatingState}", 4)
    }

	if (mode == "heat") {
//		if (temperature >= heatingSetpoint) {
		if (operatingState == 'fan only') {
        	statusText = 'Fan Only'
        } else if (operatingState.startsWith('heating')) {
			statusText = 'Heating '
            if (operatingState.contains('sma')) {
            	statusText += '(Smart Recovery)'
            } else {
            	statusText += "to ${heatingSetpoint}°"
            }
		} else {
        	// asert operatingState == 'idle'
			statusText = "Heating at ${heatingSetpoint}°"
		}
	} else if (mode == "cool") {
//		if (temperature <= coolingSetpoint) {
		if (operatingState == 'fan only') {
        	statusText = 'Fan Only'
		} else if (operatingState.startsWith('cooling')) {
        	statusText = 'Cooling '
            if (operatingState.contains('sma')) {
            	statusText += '(Smart Recovery)'
            } else {
            	statusText += "to ${coolingSetpoint}°"
            }
		} else {
			statusText = "Cooling at ${coolingSetpoint}°"
		}
	} else if (mode == 'auto') {
		if (operatingState == 'fan only') {
        	statusText = 'Fan Only'
    	} else if (operatingState.startsWith('heating')) {
        	statusText = 'Heating '
            if (operatingState.contains('sma')) {
            	statusText += '(Smart Recovery/Auto)'
            } else {
            	statusText += "to ${heatingSetpoint}° (Auto)"
            }
        } else if (operatingState.startsWith('cooling')) {
        	statusText = 'Cooling '
            if (operatingState.contains('sma')) { 
            	statusText += '(Smart Recovery/Auto)'
        	} else {
            	statusText += "to ${coolingSetpoint}° (Auto)"
            }
        } else {
			statusText = "Idle (Auto ${heatingSetpoint}°-${coolingSetpoint}°)"
        }
	} else if (mode == "off") {
		statusText = "Right Now: Off"
	} else if (mode == "emergencyHeat" || mode == "emergency heat" || mode == "emergency") {
    	if (operatingState != "heating") {
			statusText = "Idle (Emergency Heat)"
		} else {
			statusText = "Emergency Heating to ${heatingSetpoint}°"
		}
	} else {
		statusText = "${mode}?"
	}
	LOG("Generate Status Event = ${statusText}", 4)
	sendEvent(name:"thermostatStatus", value:statusText, description:statusText, displayed: false)
}

// generate custom mobile activity feeds event
// (Need to clean this up to remove as many characters as possible, else it isn't readable in the Mobile App
def generateActivityFeedsEvent(notificationMessage) {
	sendEvent(name: "notificationMessage", value: "${device.displayName} ${notificationMessage}", descriptionText: "${device.displayName} ${notificationMessage}", displayed: true)
}

def noOp() {
	// Doesn't do anything. Here due to a formatting issue on the Tiles!
}

def getSliderRange() {
	// should be returning the attributes heatRange and coolRange (once they are populated), but you can't get access to those while the forms are created (even after running for days).
	// return "'\${wantMetric()}'" ? "(5..35)" : "(45..95)"
    return "(5..90)" 
}

// Built in functions from SmartThings
// getTemperatureScale()
// fahrenheitToCelsius()
// celsiusToFahrenheit()

def wantMetric() {
	return (getTemperatureScale() == "C")
}

private def cToF(temp) {
    return celsiusToFahrenheit(temp)
}
private def fToC(temp) {
    return fahrenheitToCelsius(temp)
}

private def getImageURLRoot() {
	return "https://raw.githubusercontent.com/StrykerSKS/SmartThings/master/smartapp-icons/ecobee/dark/"
}

private def getDeviceId() {
	def deviceId = device.deviceNetworkId.split(/\./).last()	
    LOG("getDeviceId() returning ${deviceId}", 4)
    return deviceId
}

private def usingSmartAuto() {
	LOG("Entered usingSmartAuto() ", 5)
	if (settings.smartAuto) { return settings.smartAuto }
    if (parent.settings.smartAuto) { return parent.settings.smartAuto }
    return false
}

private def whatHoldType() {
	if (settings.holdType && settings.holdType.toString() != '') { 
    	// Use the hard-coded preferences, if specified - this allows each thermostat to have a different value - not sure if this is logically correct though.
    	def sendHoldType = (settings.holdType=='Temporary') ? 'nextTransition' : 'indefinite'
        LOG("Using ${device.displayName} holdType: ${sendHoldType}",2,this,'info')
        return sendHoldType
    } else {
    	// figure out what our parent expects
 		def sendHoldType = (parent.settings.holdType && parent.settings.holdType.toString() != '') ? ((parent.settings.holdType=='Until Next Program') ? 'nextTransition' : 'indefinite') : 'indefinite'
        LOG("Using ${parent.displayName} holdType: ${sendHoldType}",2,this,'info')
        return sendHoldType
    }
}

private debugLevel(level=3) {
	Integer dbg = device.currentValue('debugLevel')?.toInteger()
	Integer debugLvlNum = dbg ? dbg : (parent.settings.debugLevel ? parent.settings.debugLevel.toInteger() : 3)
    return ( debugLvlNum >= level?.toInteger() )
}

private def LOG(message, level=3, child=null, logType="debug", event=false, displayEvent=false) {
	def prefix = debugLevel(5) ? 'LOG: ' : ''
	if (debugLevel(level)) { 
    	log."${logType}" "${prefix}${message}"
        if (event) { debugEvent(message, displayEvent) }        
	}  
}

private def debugEvent(message, displayEvent = false) {
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	if ( debugLevel(4) ) { log.debug "Generating AppDebug Event: ${results}" }
	sendEvent (results)
}

def getTempColors() {
	def colorMap

	colorMap = [
		// Celsius Color Range
		[value: 0, color: "#1e9cbb"],
		[value: 15, color: "#1e9cbb"],
		[value: 19, color: "#1e9cbb"],

		[value: 21, color: "#44b621"],
		[value: 22, color: "#44b621"],
		[value: 24, color: "#44b621"],

		[value: 21, color: "#d04e00"],
		[value: 35, color: "#d04e00"],
		[value: 37, color: "#d04e00"],
		// Fahrenheit Color Range
		[value: 40, color: "#1e9cbb"],
		[value: 59, color: "#1e9cbb"],
		[value: 67, color: "#1e9cbb"],

		[value: 69, color: "#44b621"],
		[value: 72, color: "#44b621"],
		[value: 74, color: "#44b621"],

		[value: 76, color: "#d04e00"],
		[value: 95, color: "#d04e00"],
		[value: 99, color: "#d04e00"],
        
        [value: 451, color: "#ffa81e"] // Nod to the book and temp that paper burns. Used to catch when the device is offline
	]
}

def getStockTempColors() {
	def colorMap
    
    colorMap = [
    	[value: 31, color: "#153591"],
        [value: 44, color: "#1e9cbb"],
        [value: 59, color: "#90d2a7"],
        [value: 74, color: "#44b621"],
        [value: 84, color: "#f1d801"],
        [value: 95, color: "#d04e00"],
        [value: 96, color: "#bc2323"]
    ]       
}
