package org.alicebot.ab.llm.dto.ollama;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Options {
	private boolean vocab_only;
	private int seed;
	private int mirostat;
	private boolean f16_kv;
	private double presence_penalty;
	private int num_batch;
	private boolean penalize_new_line;
	private double top_p;
	private double frequency_penalty;
	private int top_k;
	private double temperature;
	private boolean use_mmap;
	private int repeat_last_n;
	private double mirostat_eta;
	private int main_gpu;
	private int num_thread;
	private boolean low_vram;
	private boolean numa;
	private int num_predict;
	private double tfs_z;
	private int num_ctx;
	private double min_p;
	private int num_gpu;
	private boolean use_mlock;
	private List<String> stop;
	private double mirostat_tau;
	private double repeatPenalty;
	private int num_keep;
	private double typical_p;

	public boolean isVocab_only() {
		return vocab_only;
	}

	public void setVocab_only(boolean vocab_only) {
		this.vocab_only = vocab_only;
	}

	public int getSeed() {
		return seed;
	}

	public void setSeed(int seed) {
		this.seed = seed;
	}

	public int getMirostat() {
		return mirostat;
	}

	public void setMirostat(int mirostat) {
		this.mirostat = mirostat;
	}

	public boolean isF16_kv() {
		return f16_kv;
	}

	public void setF16_kv(boolean f16_kv) {
		this.f16_kv = f16_kv;
	}

	public double getPresence_penalty() {
		return presence_penalty;
	}

	public void setPresence_penalty(double presence_penalty) {
		this.presence_penalty = presence_penalty;
	}

	public int getNum_batch() {
		return num_batch;
	}

	public void setNum_batch(int num_batch) {
		this.num_batch = num_batch;
	}

	public boolean isPenalize_new_line() {
		return penalize_new_line;
	}

	public void setPenalize_new_line(boolean penalize_new_line) {
		this.penalize_new_line = penalize_new_line;
	}

	public double getTop_p() {
		return top_p;
	}

	public void setTop_p(double top_p) {
		this.top_p = top_p;
	}

	public double getFrequency_penalty() {
		return frequency_penalty;
	}

	public void setFrequency_penalty(double frequency_penalty) {
		this.frequency_penalty = frequency_penalty;
	}

	public int getTop_k() {
		return top_k;
	}

	public void setTop_k(int top_k) {
		this.top_k = top_k;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public boolean isUse_mmap() {
		return use_mmap;
	}

	public void setUse_mmap(boolean use_mmap) {
		this.use_mmap = use_mmap;
	}

	public int getRepeat_last_n() {
		return repeat_last_n;
	}

	public void setRepeat_last_n(int repeat_last_n) {
		this.repeat_last_n = repeat_last_n;
	}

	public double getMirostat_eta() {
		return mirostat_eta;
	}

	public void setMirostat_eta(double mirostat_eta) {
		this.mirostat_eta = mirostat_eta;
	}

	public int getMain_gpu() {
		return main_gpu;
	}

	public void setMain_gpu(int main_gpu) {
		this.main_gpu = main_gpu;
	}

	public int getNum_thread() {
		return num_thread;
	}

	public void setNum_thread(int num_thread) {
		this.num_thread = num_thread;
	}

	public boolean isLow_vram() {
		return low_vram;
	}

	public void setLow_vram(boolean low_vram) {
		this.low_vram = low_vram;
	}

	public boolean isNuma() {
		return numa;
	}

	public void setNuma(boolean numa) {
		this.numa = numa;
	}

	public int getNum_predict() {
		return num_predict;
	}

	public void setNum_predict(int num_predict) {
		this.num_predict = num_predict;
	}

	public double getTfs_z() {
		return tfs_z;
	}

	public void setTfs_z(double tfs_z) {
		this.tfs_z = tfs_z;
	}

	public int getNum_ctx() {
		return num_ctx;
	}

	public void setNum_ctx(int num_ctx) {
		this.num_ctx = num_ctx;
	}

	public double getMin_p() {
		return min_p;
	}

	public void setMin_p(double min_p) {
		this.min_p = min_p;
	}

	public int getNum_gpu() {
		return num_gpu;
	}

	public void setNum_gpu(int num_gpu) {
		this.num_gpu = num_gpu;
	}

	public boolean isUse_mlock() {
		return use_mlock;
	}

	public void setUse_mlock(boolean use_mlock) {
		this.use_mlock = use_mlock;
	}

	public List<String> getStop() {
		return stop;
	}

	public void setStop(List<String> stop) {
		this.stop = stop;
	}

	public double getMirostat_tau() {
		return mirostat_tau;
	}

	public void setMirostat_tau(double mirostat_tau) {
		this.mirostat_tau = mirostat_tau;
	}

	public double getRepeatPenalty() {
		return repeatPenalty;
	}

	public void setRepeatPenalty(double repeatPenalty) {
		this.repeatPenalty = repeatPenalty;
	}

	public int getNum_keep() {
		return num_keep;
	}

	public void setNum_keep(int num_keep) {
		this.num_keep = num_keep;
	}

	public double getTypical_p() {
		return typical_p;
	}

	public void setTypical_p(double typical_p) {
		this.typical_p = typical_p;
	}

	@Override
	public String toString() {
		return "Options{" +
				"vocab_only=" + vocab_only +
				", seed=" + seed +
				", mirostat=" + mirostat +
				", f16_kv=" + f16_kv +
				", presence_penalty=" + presence_penalty +
				", num_batch=" + num_batch +
				", penalize_new_line=" + penalize_new_line +
				", top_p=" + top_p +
				", frequency_penalty=" + frequency_penalty +
				", top_k=" + top_k +
				", temperature=" + temperature +
				", use_mmap=" + use_mmap +
				", repeat_last_n=" + repeat_last_n +
				", mirostat_eta=" + mirostat_eta +
				", main_gpu=" + main_gpu +
				", num_thread=" + num_thread +
				", low_vram=" + low_vram +
				", numa=" + numa +
				", num_predict=" + num_predict +
				", tfs_z=" + tfs_z +
				", num_ctx=" + num_ctx +
				", min_p=" + min_p +
				", num_gpu=" + num_gpu +
				", use_mlock=" + use_mlock +
				", stop=" + stop +
				", mirostat_tau=" + mirostat_tau +
				", repeatPenalty=" + repeatPenalty +
				", num_keep=" + num_keep +
				", typical_p=" + typical_p +
				'}';
	}
}