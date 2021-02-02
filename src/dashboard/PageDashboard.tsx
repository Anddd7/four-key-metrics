import React, { useState, useEffect } from "react";
import { SettingOutlined, FullscreenOutlined, SyncOutlined } from "@ant-design/icons";
import { Typography, Button, DatePicker, Row, Col, Form, Radio } from "antd";
import { SECONDARY_COLOR, PRIMARY_COLOR } from "../constants/styles";
import { css } from "@emotion/react";
import moment from "moment";
import { dateFormatYYYYMMDD } from "../constants/date-format";
import { MultipleCascadeSelect, Option } from "../components/MultipleCascadeSelect";
import { EditableText } from "../components/EditableText";
import { useQuery } from "../hooks/useQuery";
import {
	updateDashboardNameUsingPut,
	getLastSynchronizationUsingGet,
	updateBuildsUsingPost,
	getPipelineStagesUsingGet,
	PipelineStagesResponse,
} from "../clients/apis";
import { formatLastUpdateTime } from "../utils/timeFormats";

const { Text } = Typography;
const { RangePicker } = DatePicker;

const containerStyles = css({
	padding: "29px 32px",
});

const dividerStyles = css({
	display: "inline-block",
	borderRight: `1px solid ${SECONDARY_COLOR}`,
	padding: "4px 24px 4px 0",
});

const settingStyles = css({
	fontSize: 16,
	padding: "5px 0",
	cursor: "pointer",
});

const settingTextStyles = css({
	marginLeft: 10,
});

const fullScreenStyles = css({
	backgroundColor: SECONDARY_COLOR,
	fontSize: 16,
	borderRadius: 4,
	padding: 10,
	marginLeft: 24,
	cursor: "pointer",
});

const fullScreenIconStyles = css({
	color: PRIMARY_COLOR,
});

const headerStyles = css({
	display: "flex",
	alignItems: "center",
	justifyContent: "space-between",
});

const fullScreenTextStyles = css({ marginLeft: 10, color: PRIMARY_COLOR });

export const PageDashboard = () => {
	const [syncing, setSyncing] = useState(false);
	const query = useQuery();
	const dashboardId = query.get("dashboardId") || "";
	const dashboardName = query.get("dashboardName") || "";
	const [lastModifyDateTime, setLastModifyDateTime] = useState("");
	const [pipelineStages, setPipelineStages] = useState<Option[]>([]);

	const syncBuilds = () => {
		setSyncing(true);
		updateBuildsUsingPost({
			dashboardId,
		})
			.then(() => {
				window.location.reload();
			})
			.finally(() => {
				setSyncing(false);
			});
	};

	const updateDashboardName = (name: string) =>
		updateDashboardNameUsingPut({
			dashboardId,
			requestBody: name,
		});

	useEffect(() => {
		getLastSynchronizationUsingGet({ dashboardId }).then((resp: any) => {
			setLastModifyDateTime(formatLastUpdateTime(resp.data.synchronizationTimestamp));
			return resp;
		});
		getPipelineStagesUsingGet({ dashboardId }).then((resp: any) => {
			setPipelineStages(
				resp.data.map((v: PipelineStagesResponse) => ({
					label: v.pipelineName,
					value: v.pipelineName, // TODO: set pipelineId later
					children: v.stages.map((stageName: string) => ({
						label: stageName,
						value: stageName,
					})),
				}))
			);
		});
	}, []);

	return (
		<div css={containerStyles}>
			<div css={headerStyles}>
				<div>
					<EditableText defaultValue={dashboardName} onEditDone={updateDashboardName} />
					<Text type={"secondary"}>The latest available data end at : {lastModifyDateTime}</Text>
					<Button type="link" icon={<SyncOutlined />} loading={syncing} onClick={syncBuilds}>
						{syncing ? "Synchronizing...." : "Sync Data"}
					</Button>
				</div>
				<div>
					<span css={dividerStyles}>
						<span css={settingStyles}>
							<SettingOutlined />
							<Text css={settingTextStyles}>Pipeline Setting</Text>
						</span>
					</span>
					<span css={fullScreenStyles}>
						<FullscreenOutlined css={fullScreenIconStyles} />
						<Text css={fullScreenTextStyles}>Full Screen</Text>
					</span>
				</div>
			</div>
			<div css={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
				<Form
					layout={"vertical"}
					css={{ marginTop: 16, width: "50%" }}
					initialValues={{
						duration: [
							moment(new Date(), dateFormatYYYYMMDD).startOf("day"),
							moment(new Date(), dateFormatYYYYMMDD).endOf("day").subtract(4, "month"),
						],
					}}
					onFinish={values => {
						console.log("submit", values);
					}}>
					<Row wrap={false} gutter={12}>
						<Col>
							<Form.Item label="Duration" name="duration">
								<RangePicker format={dateFormatYYYYMMDD} clearIcon={false} />
							</Form.Item>
						</Col>
						<Col span={10}>
							<Form.Item label="Pipelines" name="pipelines">
								<MultipleCascadeSelect
									options={pipelineStages}
									defaultValues={
										pipelineStages.length > 0
											? [
													{
														value: pipelineStages[0]?.value,
														childValue: (pipelineStages[0]?.children ?? [])[0]?.label,
													},
											  ]
											: []
									}
								/>
							</Form.Item>
						</Col>
						<Col style={{ textAlign: "right" }}>
							<Form.Item label=" ">
								<Button htmlType="submit" disabled={syncing}>
									Apply
								</Button>
							</Form.Item>
						</Col>
					</Row>
				</Form>

				<Radio.Group defaultValue="fortnightly">
					<Radio.Button value="fortnightly">Fortnightly</Radio.Button>
					<Radio.Button value="monthly">Monthly</Radio.Button>
				</Radio.Group>
			</div>
		</div>
	);
};
