package javabot;

import javabot.model.Region;

public class State {
	
	Region which;
	State former;
	
	public State(Region _which, State _former) {
		which=_which;
		former=_former;
	}
	
}
