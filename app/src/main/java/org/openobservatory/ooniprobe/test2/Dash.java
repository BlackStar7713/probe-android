package org.openobservatory.ooniprobe.test2;

import org.openobservatory.ooniprobe.activity.AbstractActivity;
import org.openobservatory.ooniprobe.model.JsonResult;
import org.openobservatory.ooniprobe.model.Summary;
import org.openobservatory.ooniprobe.model.Test;

import static org.openobservatory.ooniprobe.model.Measurement.MeasurementState.measurementFailed;

public class Dash extends AbstractTest<JsonResult> {
	public Dash(AbstractActivity activity) {
		super(activity, Test.DASH, new org.openobservatory.measurement_kit.nettests.DashTest(), JsonResult.class);
	}

	/*
     onEntry method for dash test, check "failure" key
     !=null => failed
     */
	@Override public void onEntry(JsonResult json) {
		super.onEntry(json);
		if (json != null) {
			JsonResult.TestKeys keys = json.test_keys;
			if (keys.failure != null)
				measurement.state = measurementFailed;
			Summary summary = result.getSummary();
			summary.dash = keys;
			super.updateSummary();
			measurement.save();
		}
	}
}
