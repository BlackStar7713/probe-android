package org.openobservatory.ooniprobe.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;

import com.google.android.material.tabs.TabLayout;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.openobservatory.ooniprobe.R;
import org.openobservatory.ooniprobe.fragment.resultHeader.ResultHeaderDetailFragment;
import org.openobservatory.ooniprobe.fragment.resultHeader.ResultHeaderMiddleboxFragment;
import org.openobservatory.ooniprobe.fragment.resultHeader.ResultHeaderPerformanceFragment;
import org.openobservatory.ooniprobe.fragment.resultHeader.ResultHeaderTBAFragment;
import org.openobservatory.ooniprobe.item.MeasurementItem;
import org.openobservatory.ooniprobe.item.MeasurementPerfItem;
import org.openobservatory.ooniprobe.model.database.Measurement;
import org.openobservatory.ooniprobe.model.database.Network;
import org.openobservatory.ooniprobe.model.database.Result;
import org.openobservatory.ooniprobe.model.database.Result_Table;
import org.openobservatory.ooniprobe.test.suite.AbstractSuite;
import org.openobservatory.ooniprobe.test.suite.InstantMessagingSuite;
import org.openobservatory.ooniprobe.test.suite.MiddleBoxesSuite;
import org.openobservatory.ooniprobe.test.suite.PerformanceSuite;
import org.openobservatory.ooniprobe.test.suite.WebsitesSuite;
import org.openobservatory.ooniprobe.test.test.AbstractTest;
import org.openobservatory.ooniprobe.test.test.WebConnectivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import localhost.toolkit.app.ConfirmDialogFragment;
import localhost.toolkit.widget.HeterogeneousRecyclerAdapter;
import localhost.toolkit.widget.HeterogeneousRecyclerItem;

public class ResultDetailActivity extends AbstractActivity implements View.OnClickListener, ConfirmDialogFragment.OnConfirmedListener {
	public static final String ID = "id";
	@BindView(R.id.toolbar) Toolbar toolbar;
	@BindView(R.id.tabLayout) TabLayout tabLayout;
	@BindView(R.id.pager) ViewPager pager;
	@BindView(R.id.recyclerView) RecyclerView recycler;
	private Result result;

	public static Intent newIntent(Context context, int id) {
		return new Intent(context, ResultDetailActivity.class).putExtra(ID, id);
	}

	@Override protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		result = SQLite.select().from(Result.class).where(Result_Table.id.eq(getIntent().getIntExtra(ID, 0))).querySingle();
		assert result != null;
		setTheme(result.getTestSuite().getThemeLight());
		setContentView(R.layout.activity_result_detail);
		ButterKnife.bind(this);
		setSupportActionBar(toolbar);
		ActionBar bar = getSupportActionBar();
		if (bar != null) {
			bar.setDisplayHomeAsUpEnabled(true);
			bar.setTitle(DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "yMdHm"), result.start_time));
		}
		pager.setAdapter(new ResultHeaderAdapter());
		tabLayout.setupWithViewPager(pager);
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		recycler.setLayoutManager(layoutManager);
		recycler.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));
		ArrayList<HeterogeneousRecyclerItem> items = new ArrayList<>();
		boolean isPerf = result.test_group_name.equals(PerformanceSuite.NAME);
		for (Measurement measurement : result.getMeasurements())
			items.add(isPerf ? new MeasurementPerfItem(measurement, this) : new MeasurementItem(measurement, this));
		recycler.setAdapter(new HeterogeneousRecyclerAdapter<>(this, items));
		result.is_viewed = true;
		result.save();
	}

	@Override public void onClick(View v) {
		Measurement measurement = (Measurement) v.getTag();
		if (measurement.is_failed)
			ConfirmDialogFragment.newInstance(measurement, getString(R.string.Modal_ReRun_Title), getString(R.string.Modal_ReRun_Paragraph)).show(getSupportFragmentManager(), null);
		else
			startActivity(MeasurementDetailActivity.newIntent(this, measurement.id));
	}

	@Override public void onConfirmation(Serializable serializable, int i) {
		if (i == DialogInterface.BUTTON_POSITIVE) {
			Measurement failedMeasurement = (Measurement) serializable;
			failedMeasurement.result.load();
			AbstractTest abstractTest = failedMeasurement.getTest();
			if (abstractTest instanceof WebConnectivity)
				((WebConnectivity) abstractTest).setInputs(Collections.singletonList(failedMeasurement.url.url));
			AbstractSuite testSuite = failedMeasurement.result.getTestSuite();
			testSuite.setTestList(abstractTest);
			testSuite.setResult(failedMeasurement.result);
			failedMeasurement.is_rerun = true;
			failedMeasurement.save();
			Intent intent = RunningActivity.newIntent(this, testSuite);
			if (intent != null) {
				startActivity(intent);
				finish();
			}
		}
	}

	private class ResultHeaderAdapter extends FragmentPagerAdapter {
		ResultHeaderAdapter() {
			super(getSupportFragmentManager());
		}

		@Override public Fragment getItem(int position) {
			if (position == 1) {
				Network network = result.network;
				return ResultHeaderDetailFragment.newInstance(result.getFormattedDataUsageUp(), result.getFormattedDataUsageDown(), null, result.runtime, true, Network.getCountry(ResultDetailActivity.this, network), Network.toString(ResultDetailActivity.this, network, 2));
			} else switch (result.test_group_name) {
				case WebsitesSuite.NAME:
					return ResultHeaderTBAFragment.newInstance(result, R.plurals.TestResults_Summary_Websites_Hero_Sites);
				case InstantMessagingSuite.NAME:
					return ResultHeaderTBAFragment.newInstance(result, R.plurals.TestResults_Summary_InstantMessaging_Hero_Apps);
				case MiddleBoxesSuite.NAME:
					return ResultHeaderMiddleboxFragment.newInstance(result.countAnomalousMeasurements() > 0);
				case PerformanceSuite.NAME:
					return ResultHeaderPerformanceFragment.newInstance(result);
				default:
					return null;
			}
		}

		@Override public int getCount() {
			return 2;
		}

		@Nullable @Override public CharSequence getPageTitle(int position) {
			return "●";
		}
	}
}
