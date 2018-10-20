package org.openobservatory.ooniprobe.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.openobservatory.ooniprobe.R;
import org.openobservatory.ooniprobe.model.database.Result;
import org.openobservatory.ooniprobe.test.suite.AbstractSuite;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ru.noties.markwon.Markwon;

public class OverviewActivity extends AbstractActivity {
	public static final String TEST = "test";
	@BindView(R.id.toolbar) Toolbar toolbar;
	@BindView(R.id.icon) ImageView icon;
	@BindView(R.id.title) TextView title;
	@BindView(R.id.runtime) TextView runtime;
	@BindView(R.id.lastTime) TextView lastTime;
	@BindView(R.id.configure) Button configure;
	@BindView(R.id.desc) TextView desc;
	private AbstractSuite testSuite;

	public static Intent newIntent(Context context, AbstractSuite testSuite) {
		return new Intent(context, OverviewActivity.class).putExtra(TEST, testSuite);
	}

	@Override protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		testSuite = (AbstractSuite) getIntent().getSerializableExtra(TEST);
		setTheme(testSuite.getThemeLight());
		setContentView(R.layout.activity_overview);
		ButterKnife.bind(this);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		icon.setImageResource(testSuite.getIcon());
		title.setText(testSuite.getTitle());
		runtime.setText(testSuite.getDataUsage() + " " + DateUtils.formatElapsedTime(testSuite.getRuntime()));
		Markwon.setMarkdown(desc, getString(testSuite.getDesc1()) + "\n\n" + getString(testSuite.getDesc2()));
		Result lastResult = Result.getLastResult(testSuite.getName());
		if (lastResult != null)
			lastTime.setText(DateUtils.getRelativeTimeSpanString(lastResult.start_time.getTime()));
	}

	@OnClick(R.id.configure) void onConfigureClick() {
		startActivity(PreferenceActivity.newIntent(this, testSuite.getPref()));
	}

	@OnClick(R.id.run) void onRunClick() {
		Intent intent = RunningActivity.newIntent(this, testSuite);
		if (intent != null)
			startActivity(intent);
	}
}
