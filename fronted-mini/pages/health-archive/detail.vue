<template>
	<view :class="['container', rootFontClass]">
		<!-- 自定义导航栏 -->
		<view class="custom-navbar">
			<view class="navbar-content">
				<view class="navbar-left" @click="goBack">
					<uni-icons type="left" size="20" color="#333333"></uni-icons>
				</view>
				<view class="navbar-title">健康档案详情</view>
				<view class="navbar-right">
					<text v-if="!isEditMode" class="edit-btn" @click="enterEditMode">编辑</text>
					<text v-else class="save-btn" @click="saveArchive">保存</text>
				</view>
			</view>
		</view>
		
		<!-- 加载中 -->
		<view v-if="loading" class="loading-container">
			<uni-load-more status="loading"></uni-load-more>
		</view>

		<!-- 健康档案详情 -->
		<scroll-view v-else scroll-y class="detail-scroll">
			<!-- 健康档案主表信息 -->
			<view class="detail-section" v-if="detailData.healthArchive || isEditMode">
				<view class="section-title" @click="toggleSection('healthArchive')">
					<view class="title-left">
						<uni-icons type="folder" size="20" color="#07C160"></uni-icons>
						<text class="title-text">健康档案基本信息</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('healthArchive') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('healthArchive')">
					<view class="info-row" @click="handleRowClick('healthArchive', 'userName')">
						<text class="info-label">档案姓名：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'healthArchive.userName') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthArchive.userName')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.healthArchive.userName" 
							placeholder="请输入档案姓名"
							@blur="updateField('healthArchive', 'userName', editData.healthArchive.userName)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('healthArchive', 'archiveNo')">
						<text class="info-label">档案编号：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'healthArchive.archiveNo') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthArchive.archiveNo')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.healthArchive.archiveNo" 
							placeholder="请输入档案编号"
							@blur="updateField('healthArchive', 'archiveNo', editData.healthArchive.archiveNo)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('healthArchive', 'archiveName')">
						<text class="info-label">档案名称：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'healthArchive.archiveName') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthArchive.archiveName')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.healthArchive.archiveName" 
							placeholder="请输入档案名称"
							@blur="updateField('healthArchive', 'archiveName', editData.healthArchive.archiveName)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('healthArchive', 'archiveDate')">
						<text class="info-label">档案日期：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'healthArchive.archiveDate') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthArchive.archiveDate')) }}
							</text>
						</view>
						<picker v-else 
							mode="date" 
							:value="editData.healthArchive.archiveDate || ''" 
							@change="onDateChange('healthArchive', 'archiveDate', $event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !editData.healthArchive.archiveDate && 'placeholder']">
									{{ editData.healthArchive.archiveDate || '请选择档案日期' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" @click="handleRowClick('healthArchive', 'archiveYear')">
						<text class="info-label">档案年份：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'healthArchive.archiveYear') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthArchive.archiveYear')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							type="number"
							v-model="editData.healthArchive.archiveYear" 
							placeholder="请输入档案年份"
							@blur="updateField('healthArchive', 'archiveYear', editData.healthArchive.archiveYear)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('healthArchive', 'archiveManagerName')">
						<text class="info-label">管理机构名称：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'healthArchive.archiveManagerName') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthArchive.archiveManagerName')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.healthArchive.archiveManagerName" 
							placeholder="请输入管理机构名称"
							@blur="updateField('healthArchive', 'archiveManagerName', editData.healthArchive.archiveManagerName)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('healthArchive', 'archiveManagerPhone')">
						<text class="info-label">机构电话：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'healthArchive.archiveManagerPhone') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthArchive.archiveManagerPhone')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							type="number"
							v-model="editData.healthArchive.archiveManagerPhone" 
							placeholder="请输入机构电话"
							@blur="updateField('healthArchive', 'archiveManagerPhone', editData.healthArchive.archiveManagerPhone)"
						/>
					</view>
				</view>
			</view>

			<!-- 个人健康标识 -->
			<view class="detail-section" v-if="detailData.healthProfileTags || isEditMode">
				<view class="section-title" @click="toggleSection('healthProfileTags')">
					<view class="title-left">
						<uni-icons type="flag" size="20" color="#07C160"></uni-icons>
						<text class="title-text">个人健康标识</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('healthProfileTags') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('healthProfileTags')">
					<view class="info-row">
						<text class="info-label">0-6岁儿童：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', safeGet(detailData, 'healthProfileTags.isChild06') === null && 'unknown-value']">
								{{ safeGet(detailData, 'healthProfileTags.isChild06') === null ? '未知' : (safeGet(detailData, 'healthProfileTags.isChild06') ? '是' : '否') }}
							</text>
						</view>
						<switch v-else 
							:checked="safeGet(editData, 'healthProfileTags.isChild06') || false"
							@change="updateField('healthProfileTags', 'isChild06', $event.detail.value)"
							color="#07C160"
						/>
					</view>
					<view class="info-row">
						<text class="info-label">65岁以上：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', safeGet(detailData, 'healthProfileTags.isElderly65') === null && 'unknown-value']">
								{{ safeGet(detailData, 'healthProfileTags.isElderly65') === null ? '未知' : (safeGet(detailData, 'healthProfileTags.isElderly65') ? '是' : '否') }}
							</text>
						</view>
						<switch v-else 
							:checked="safeGet(editData, 'healthProfileTags.isElderly65') || false"
							@change="updateField('healthProfileTags', 'isElderly65', $event.detail.value)"
							color="#07C160"
						/>
					</view>
					<view class="info-row">
						<text class="info-label">孕产妇：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', safeGet(detailData, 'healthProfileTags.isPregnant') === null && 'unknown-value']">
								{{ safeGet(detailData, 'healthProfileTags.isPregnant') === null ? '未知' : (safeGet(detailData, 'healthProfileTags.isPregnant') ? '是' : '否') }}
							</text>
						</view>
						<switch v-else 
							:checked="safeGet(editData, 'healthProfileTags.isPregnant') || false"
							@change="onPregnantChange($event.detail.value)"
							color="#07C160"
						/>
					</view>
					<view class="info-row" v-if="isEditMode || safeGet(detailData, 'healthProfileTags.isPregnant')">
						<text class="info-label">风险等级：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isFieldEmpty(safeGet(detailData, 'healthProfileTags.pregnancyRisk')) && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthProfileTags.pregnancyRisk')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="pregnancyRiskOptions" 
							:value="getPregnancyRiskIndex()"
							@change="onPregnancyRiskChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'healthProfileTags.pregnancyRisk') && 'placeholder']">
									{{ safeGet(editData, 'healthProfileTags.pregnancyRisk') || '请选择风险等级' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row">
						<text class="info-label">体重状况：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isFieldEmpty(safeGet(detailData, 'healthProfileTags.weightStatus')) && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthProfileTags.weightStatus')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="weightStatusOptions" 
							:value="getWeightStatusIndex()"
							@change="onWeightStatusChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'healthProfileTags.weightStatus') && 'placeholder']">
									{{ safeGet(editData, 'healthProfileTags.weightStatus') || '请选择体重状况' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row">
						<text class="info-label">血型：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isFieldEmpty(safeGet(detailData, 'healthProfileTags.bloodType')) && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'healthProfileTags.bloodType')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="bloodTypeOptions" 
							:value="getBloodTypeIndex()"
							@change="onBloodTypeChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'healthProfileTags.bloodType') && 'placeholder']">
									{{ safeGet(editData, 'healthProfileTags.bloodType') || '请选择血型' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row">
						<text class="info-label">慢性病/重点疾病：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', (!chronicDiseaseList || chronicDiseaseList.length === 0) && 'unknown-value']">
								{{ (chronicDiseaseList && chronicDiseaseList.length > 0) ? chronicDiseaseList.join('、') : '未知' }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editChronicDiseaseText" 
							placeholder="请输入慢性病，多个用逗号分隔"
							@blur="updateChronicDisease"
						/>
					</view>
					<view class="info-row">
						<text class="info-label">法定传染病：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', (!statutoryInfoList || statutoryInfoList.length === 0) && 'unknown-value']">
								{{ (statutoryInfoList && statutoryInfoList.length > 0) ? statutoryInfoList.join('、') : '未知' }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editStatutoryInfoText" 
							placeholder="请输入法定传染病，多个用逗号分隔"
							@blur="updateStatutoryInfo"
						/>
					</view>
				</view>
			</view>

			<!-- 用户基本信息 -->
			<view class="detail-section" v-if="detailData.userInfo || isEditMode">
				<view class="section-title" @click="toggleSection('userInfo')">
					<view class="title-left">
						<uni-icons type="person" size="20" color="#07C160"></uni-icons>
						<text class="title-text">用户基本信息</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('userInfo') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('userInfo')">
					<view class="info-row" @click="handleRowClick('userInfo', 'fullName')">
						<text class="info-label">姓名：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.fullName') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.fullName')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.fullName" 
							placeholder="请输入姓名"
							@blur="updateField('userInfo', 'fullName', editData.userInfo.fullName)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'gender')">
						<text class="info-label">性别：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.gender') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.gender')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="genderOptions" 
							:value="getGenderIndex()"
							@change="onGenderChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userInfo.gender') && 'placeholder']">
									{{ safeGet(editData, 'userInfo.gender') || '请选择性别' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'birthDate')">
						<text class="info-label">出生日期：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.birthDate') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.birthDate')) }}
							</text>
						</view>
						<picker v-else 
							mode="date" 
							:value="safeGet(editData, 'userInfo.birthDate') || ''" 
							@change="onDateChange('userInfo', 'birthDate', $event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userInfo.birthDate') && 'placeholder']">
									{{ safeGet(editData, 'userInfo.birthDate') || '请选择出生日期' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'idType')">
						<text class="info-label">证件类型：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.idType') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.idType')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.idType" 
							placeholder="请输入证件类型"
							@blur="updateField('userInfo', 'idType', editData.userInfo.idType)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'idNumber')">
						<text class="info-label">证件号码：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.idNumber') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.idNumber')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.idNumber" 
							placeholder="请输入证件号码"
							@blur="updateField('userInfo', 'idNumber', editData.userInfo.idNumber)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'workSchool')">
						<text class="info-label">工作单位/学校：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.workSchool') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.workSchool')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.workSchool" 
							placeholder="请输入工作单位/学校"
							@blur="updateField('userInfo', 'workSchool', editData.userInfo.workSchool)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'nativePlace')">
						<text class="info-label">籍贯：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.nativePlace') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.nativePlace')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.nativePlace" 
							placeholder="请输入籍贯"
							@blur="updateField('userInfo', 'nativePlace', editData.userInfo.nativePlace)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'birthPlace')">
						<text class="info-label">出生地：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.birthPlace') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.birthPlace')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.birthPlace" 
							placeholder="请输入出生地"
							@blur="updateField('userInfo', 'birthPlace', editData.userInfo.birthPlace)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'ethnicity')">
						<text class="info-label">民族：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.ethnicity') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.ethnicity')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.ethnicity" 
							placeholder="请输入民族"
							@blur="updateField('userInfo', 'ethnicity', editData.userInfo.ethnicity)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'personalPhone')">
						<text class="info-label">本人电话：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.personalPhone') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.personalPhone')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							type="number"
							v-model="editData.userInfo.personalPhone" 
							placeholder="请输入本人电话"
							@blur="updateField('userInfo', 'personalPhone', editData.userInfo.personalPhone)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'residenceType')">
						<text class="info-label">常住类型：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.residenceType') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.residenceType')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="residenceTypeOptions" 
							:value="getResidenceTypeIndex()"
							@change="onResidenceTypeChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userInfo.residenceType') && 'placeholder']">
									{{ safeGet(editData, 'userInfo.residenceType') || '请选择常住类型' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'residenceAddress')">
						<text class="info-label">户籍地址/常住地址：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.residenceAddress') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.residenceAddress')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.residenceAddress" 
							placeholder="请输入户籍地址/常住地址"
							@blur="updateField('userInfo', 'residenceAddress', editData.userInfo.residenceAddress)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'educationLevel')">
						<text class="info-label">文化程度：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.educationLevel') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.educationLevel')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="educationLevelOptions" 
							:value="getEducationLevelIndex()"
							@change="onEducationLevelChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userInfo.educationLevel') && 'placeholder']">
									{{ safeGet(editData, 'userInfo.educationLevel') || '请选择文化程度' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'occupation')">
						<text class="info-label">职业：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.occupation') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.occupation')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="occupationOptions" 
							:value="getOccupationIndex()"
							@change="onOccupationChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userInfo.occupation') && 'placeholder']">
									{{ safeGet(editData, 'userInfo.occupation') || '请选择职业' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'maritalStatus')">
						<text class="info-label">婚姻状况：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.maritalStatus') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.maritalStatus')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="maritalStatusOptions" 
							:value="getMaritalStatusIndex()"
							@change="onMaritalStatusChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userInfo.maritalStatus') && 'placeholder']">
									{{ safeGet(editData, 'userInfo.maritalStatus') || '请选择婚姻状况' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'paymentMethod')">
						<text class="info-label">医疗费用支付方式：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.paymentMethod') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.paymentMethod')) }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="paymentMethodOptions" 
							:value="getPaymentMethodIndex()"
							@change="onPaymentMethodChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userInfo.paymentMethod') && 'placeholder']">
									{{ safeGet(editData, 'userInfo.paymentMethod') || '请选择医疗费用支付方式' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row">
						<text class="info-label">家庭医生签约：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', safeGet(detailData, 'userInfo.familyDoctorSigned') === null && 'unknown-value']">
								{{ safeGet(detailData, 'userInfo.familyDoctorSigned') === null ? '未知' : (safeGet(detailData, 'userInfo.familyDoctorSigned') ? '是' : '否') }}
							</text>
						</view>
						<switch v-else 
							:checked="safeGet(editData, 'userInfo.familyDoctorSigned') || false"
							@change="updateField('userInfo', 'familyDoctorSigned', $event.detail.value)"
							color="#07C160"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'familyDoctorName')">
						<text class="info-label">家庭医生姓名：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.familyDoctorName') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.familyDoctorName')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userInfo.familyDoctorName" 
							placeholder="请输入家庭医生姓名"
							@blur="updateField('userInfo', 'familyDoctorName', editData.userInfo.familyDoctorName)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userInfo', 'familyDoctorPhone')">
						<text class="info-label">家庭医生电话：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userInfo.familyDoctorPhone') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userInfo.familyDoctorPhone')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							type="number"
							v-model="editData.userInfo.familyDoctorPhone" 
							placeholder="请输入家庭医生电话"
							@blur="updateField('userInfo', 'familyDoctorPhone', editData.userInfo.familyDoctorPhone)"
						/>
					</view>
				</view>
			</view>

			<!-- 紧急联系人 -->
			<view class="detail-section" v-if="detailData.userEmergencyContacts || isEditMode">
				<view class="section-title" @click="toggleSection('userEmergencyContacts')">
					<view class="title-left">
						<uni-icons type="phone" size="20" color="#07C160"></uni-icons>
						<text class="title-text">紧急联系人</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('userEmergencyContacts') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('userEmergencyContacts')">
					<view class="info-row" @click="handleRowClick('userEmergencyContacts', 'contactName')">
						<text class="info-label">联系人姓名：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userEmergencyContacts.contactName') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userEmergencyContacts.contactName')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userEmergencyContacts.contactName" 
							placeholder="请输入联系人姓名"
							@blur="updateField('userEmergencyContacts', 'contactName', editData.userEmergencyContacts.contactName)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userEmergencyContacts', 'relationship')">
						<text class="info-label">与本人关系：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userEmergencyContacts.relationship') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userEmergencyContacts.relationship')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userEmergencyContacts.relationship" 
							placeholder="请输入与本人关系"
							@blur="updateField('userEmergencyContacts', 'relationship', editData.userEmergencyContacts.relationship)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userEmergencyContacts', 'phoneNumber')">
						<text class="info-label">联系电话：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userEmergencyContacts.phoneNumber') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userEmergencyContacts.phoneNumber')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							type="number"
							v-model="editData.userEmergencyContacts.phoneNumber" 
							placeholder="请输入联系电话"
							@blur="updateField('userEmergencyContacts', 'phoneNumber', editData.userEmergencyContacts.phoneNumber)"
						/>
					</view>
				</view>
			</view>

			<!-- 健康服务凭证 -->
			<view class="detail-section" v-if="detailData.userCertificates || isEditMode">
				<view class="section-title" @click="toggleSection('userCertificates')">
					<view class="title-left">
						<uni-icons type="paperplane" size="20" color="#07C160"></uni-icons>
						<text class="title-text">健康服务凭证</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('userCertificates') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('userCertificates')">
					<view class="info-row" @click="handleRowClick('userCertificates', 'certificateType')">
						<text class="info-label">凭证类型：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userCertificates.certificateType') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userCertificates.certificateType')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userCertificates.certificateType" 
							placeholder="请输入凭证类型"
							@blur="updateField('userCertificates', 'certificateType', editData.userCertificates.certificateType)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userCertificates', 'certificateIssuer')">
						<text class="info-label">签发机构：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userCertificates.certificateIssuer') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userCertificates.certificateIssuer')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.userCertificates.certificateIssuer" 
							placeholder="请输入签发机构"
							@blur="updateField('userCertificates', 'certificateIssuer', editData.userCertificates.certificateIssuer)"
						/>
					</view>
					<view class="info-row" @click="handleRowClick('userCertificates', 'issueDate')">
						<text class="info-label">签发时间：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userCertificates.issueDate') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'userCertificates.issueDate')) }}
							</text>
						</view>
						<picker v-else 
							mode="date" 
							:value="safeGet(editData, 'userCertificates.issueDate') || ''" 
							@change="onDateChange('userCertificates', 'issueDate', $event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userCertificates.issueDate') && 'placeholder']">
									{{ safeGet(editData, 'userCertificates.issueDate') || '请选择签发时间' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" @click="handleRowClick('userCertificates', 'expiryDate')">
						<text class="info-label">有效期至：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'userCertificates.expiryDate') && 'unknown-value']">
								{{ safeGet(detailData, 'userCertificates.expiryDate') || (safeGet(detailData, 'userCertificates.issueDate') ? '永久有效' : '未知') }}
							</text>
						</view>
						<picker v-else 
							mode="date" 
							:value="safeGet(editData, 'userCertificates.expiryDate') || ''" 
							@change="onDateChange('userCertificates', 'expiryDate', $event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'userCertificates.expiryDate') && 'placeholder']">
									{{ safeGet(editData, 'userCertificates.expiryDate') || '请选择有效期（可选，留空表示永久有效）' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
				</view>
			</view>

			<!-- 过敏史 -->
			<view class="detail-section" v-if="detailData.allergyHistory || isEditMode">
				<view class="section-title" @click="toggleSection('allergyHistory')">
					<view class="title-left">
						<uni-icons type="warning" size="20" color="#FF9500"></uni-icons>
						<text class="title-text">过敏史</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('allergyHistory') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('allergyHistory')">
					<view class="info-row">
						<text class="info-label">过敏类型：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'allergyHistory.allergyType') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'allergyHistory.allergyType'), '无') }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="allergyTypeOptions" 
							:value="getAllergyTypeIndex()"
							@change="onAllergyTypeChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'allergyHistory.allergyType') && 'placeholder']">
									{{ safeGet(editData, 'allergyHistory.allergyType') || '请选择过敏类型' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" v-if="isEditMode || safeGet(detailData, 'allergyHistory.allergyType') === '药物过敏'">
						<text class="info-label">药物过敏详情：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'allergyHistory.drugAllergyDetails') && 'unknown-value']">
								{{ formatDrugAllergy(safeGet(detailData, 'allergyHistory.drugAllergyDetails')) || '未知' }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editDrugAllergyText" 
							placeholder="请输入药物过敏详情（JSON格式）"
							@blur="updateDrugAllergy"
						/>
					</view>
					<view class="info-row" v-if="isEditMode || safeGet(detailData, 'allergyHistory.allergyType') === '食物过敏'">
						<text class="info-label">食物过敏详情：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'allergyHistory.foodAllergyDetails') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'allergyHistory.foodAllergyDetails')) }}
							</text>
						</view>
						<textarea v-else 
							class="edit-textarea" 
							v-model="editData.allergyHistory.foodAllergyDetails" 
							placeholder="请输入食物过敏详情"
							@blur="updateField('allergyHistory', 'foodAllergyDetails', editData.allergyHistory.foodAllergyDetails)"
						/>
					</view>
					<view class="info-row" v-if="isEditMode || safeGet(detailData, 'allergyHistory.allergyType') === '其他过敏'">
						<text class="info-label">其他过敏详情：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'allergyHistory.otherAllergyDetails') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'allergyHistory.otherAllergyDetails')) }}
							</text>
						</view>
						<textarea v-else 
							class="edit-textarea" 
							v-model="editData.allergyHistory.otherAllergyDetails" 
							placeholder="请输入其他过敏详情"
							@blur="updateField('allergyHistory', 'otherAllergyDetails', editData.allergyHistory.otherAllergyDetails)"
						/>
					</view>
				</view>
			</view>

			<!-- 暴露史 -->
			<view class="detail-section" v-if="detailData.exposureHistory || isEditMode">
				<view class="section-title" @click="toggleSection('exposureHistory')">
					<view class="title-left">
						<uni-icons type="info" size="20" color="#FF9500"></uni-icons>
						<text class="title-text">暴露史</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('exposureHistory') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('exposureHistory')">
					<view class="info-row">
						<text class="info-label">暴露类型：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'exposureHistory.exposureType') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'exposureHistory.exposureType'), '无') }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="exposureTypeOptions" 
							:value="getExposureTypeIndex()"
							@change="onExposureTypeChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'exposureHistory.exposureType') && 'placeholder']">
									{{ safeGet(editData, 'exposureHistory.exposureType') || '请选择暴露类型' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" v-if="isEditMode || (safeGet(detailData, 'exposureHistory.exposureType') && safeGet(detailData, 'exposureHistory.exposureType') !== '无')">
						<text class="info-label">暴露详情：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'exposureHistory.exposureDetails') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'exposureHistory.exposureDetails')) }}
							</text>
						</view>
						<textarea v-else 
							class="edit-textarea" 
							v-model="editData.exposureHistory.exposureDetails" 
							placeholder="请输入暴露详情"
							@blur="updateField('exposureHistory', 'exposureDetails', editData.exposureHistory.exposureDetails)"
						/>
					</view>
				</view>
			</view>

			<!-- 既往史 -->
			<view class="detail-section" v-if="(detailData.diseaseHistory && detailData.diseaseHistory.length > 0) || isEditMode">
				<view class="section-title" @click.stop="toggleSection('diseaseHistory')">
					<view class="title-left">
						<uni-icons type="list" size="20" color="#FF9500"></uni-icons>
						<text class="title-text">既往史</text>
					</view>
					<view class="title-right">
						<view v-if="isEditMode" class="add-btn" @click.stop="addDiseaseHistory">
							<uni-icons type="plus" size="20" color="#07C160"></uni-icons>
							<text>添加</text>
						</view>
						<view class="collapse-arrow-wrapper">
							<uni-icons 
								:type="isSectionExpanded('diseaseHistory') ? 'arrowup' : 'arrowdown'" 
								size="20" 
								color="#666666"
								class="collapse-arrow"
							></uni-icons>
						</view>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('diseaseHistory')">
					<view v-if="!isEditMode && (!detailData.diseaseHistory || detailData.diseaseHistory.length === 0)" class="empty-tip">
						<text class="unknown-value">未知</text>
					</view>
					<view class="disease-item" v-for="(disease, index) in (isEditMode ? editData.diseaseHistory : detailData.diseaseHistory)" :key="index">
						<view class="info-row">
							<text class="info-label">疾病名称：</text>
							<view v-if="!isEditMode" class="info-value-wrapper">
								<text :class="['info-value', isFieldEmpty(disease.diseaseName) && 'unknown-value']">
									{{ getFieldValue(disease.diseaseName) }}
								</text>
							</view>
							<input v-else 
								class="edit-input" 
								v-model="disease.diseaseName" 
								placeholder="请输入疾病名称"
							/>
							<view v-if="isEditMode" class="delete-btn" @click="removeDiseaseHistory(index)">
								<uni-icons type="trash" size="18" color="#ff4d4f"></uni-icons>
							</view>
						</view>
						<view class="info-row">
							<text class="info-label">发病时间：</text>
							<view v-if="!isEditMode" class="info-value-wrapper">
								<text :class="['info-value', isFieldEmpty(disease.onsetDate) && 'unknown-value']">
									{{ getFieldValue(disease.onsetDate) }}
								</text>
							</view>
							<picker v-else 
								mode="date" 
								:value="disease.onsetDate || ''" 
								@change="disease.onsetDate = $event.detail.value"
							>
								<view class="picker-view">
									<text :class="['picker-text', !disease.onsetDate && 'placeholder']">
										{{ disease.onsetDate || '请选择发病时间' }}
									</text>
									<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
								</view>
							</picker>
						</view>
						<view class="disease-divider" v-if="index < (isEditMode ? editData.diseaseHistory : detailData.diseaseHistory).length - 1"></view>
					</view>
				</view>
			</view>

			<!-- 预防接种史 -->
			<view class="detail-section" v-if="(detailData.vaccinationHistory && detailData.vaccinationHistory.length > 0) || isEditMode">
				<view class="section-title" @click.stop="toggleSection('vaccinationHistory')">
					<view class="title-left">
						<uni-icons type="checkmarkempty" size="20" color="#07C160"></uni-icons>
						<text class="title-text">预防接种史</text>
					</view>
					<view class="title-right">
						<view v-if="isEditMode" class="add-btn" @click.stop="addVaccinationHistory">
							<uni-icons type="plus" size="20" color="#07C160"></uni-icons>
							<text>添加</text>
						</view>
						<view class="collapse-arrow-wrapper">
							<uni-icons 
								:type="isSectionExpanded('vaccinationHistory') ? 'arrowup' : 'arrowdown'" 
								size="20" 
								color="#666666"
								class="collapse-arrow"
							></uni-icons>
						</view>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('vaccinationHistory')">
					<view v-if="!isEditMode && (!detailData.vaccinationHistory || detailData.vaccinationHistory.length === 0)" class="empty-tip">
						<text class="unknown-value">未知</text>
					</view>
					<view class="vaccination-item" v-for="(vaccination, index) in (isEditMode ? editData.vaccinationHistory : detailData.vaccinationHistory)" :key="index">
						<view class="info-row">
							<text class="info-label">疫苗名称：</text>
							<view v-if="!isEditMode" class="info-value-wrapper">
								<text :class="['info-value', isFieldEmpty(vaccination.vaccineName) && 'unknown-value']">
									{{ getFieldValue(vaccination.vaccineName) }}
								</text>
							</view>
							<input v-else 
								class="edit-input" 
								v-model="vaccination.vaccineName" 
								placeholder="请输入疫苗名称"
							/>
							<view v-if="isEditMode" class="delete-btn" @click="removeVaccinationHistory(index)">
								<uni-icons type="trash" size="18" color="#ff4d4f"></uni-icons>
							</view>
						</view>
						<view class="info-row">
							<text class="info-label">接种时间：</text>
							<view v-if="!isEditMode" class="info-value-wrapper">
								<text :class="['info-value', isFieldEmpty(vaccination.vaccinationDate) && 'unknown-value']">
									{{ getFieldValue(vaccination.vaccinationDate) }}
								</text>
							</view>
							<picker v-else 
								mode="date" 
								:value="vaccination.vaccinationDate || ''" 
								@change="vaccination.vaccinationDate = $event.detail.value"
							>
								<view class="picker-view">
									<text :class="['picker-text', !vaccination.vaccinationDate && 'placeholder']">
										{{ vaccination.vaccinationDate || '请选择接种时间' }}
									</text>
									<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
								</view>
							</picker>
						</view>
						<view class="disease-divider" v-if="index < (isEditMode ? editData.vaccinationHistory : detailData.vaccinationHistory).length - 1"></view>
					</view>
				</view>
			</view>

			<!-- 家族史 -->
			<view class="detail-section" v-if="(detailData.familyHistory && detailData.familyHistory.length > 0) || isEditMode">
				<view class="section-title" @click.stop="toggleSection('familyHistory')">
					<view class="title-left">
						<uni-icons type="person-filled" size="20" color="#FF9500"></uni-icons>
						<text class="title-text">家族史</text>
					</view>
					<view class="title-right">
						<view v-if="isEditMode" class="add-btn" @click.stop="addFamilyHistory">
							<uni-icons type="plus" size="20" color="#07C160"></uni-icons>
							<text>添加</text>
						</view>
						<view class="collapse-arrow-wrapper">
							<uni-icons 
								:type="isSectionExpanded('familyHistory') ? 'arrowup' : 'arrowdown'" 
								size="20" 
								color="#666666"
								class="collapse-arrow"
							></uni-icons>
						</view>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('familyHistory')">
					<view v-if="!isEditMode && (!detailData.familyHistory || detailData.familyHistory.length === 0)" class="empty-tip">
						<text class="unknown-value">未知</text>
					</view>
					<view class="family-item" v-for="(family, index) in (isEditMode ? editData.familyHistory : detailData.familyHistory)" :key="index">
						<view class="info-row">
							<text class="info-label">亲属关系：</text>
							<view v-if="!isEditMode" class="info-value-wrapper">
								<text :class="['info-value', isFieldEmpty(family.relativeType) && 'unknown-value']">
									{{ getFieldValue(family.relativeType) }}
								</text>
							</view>
							<picker v-else 
								mode="selector" 
								:range="relativeTypeOptions" 
								:value="getRelativeTypeIndex(family.relativeType)"
								@change="family.relativeType = relativeTypeOptions[$event.detail.value]"
							>
								<view class="picker-view">
									<text :class="['picker-text', !family.relativeType && 'placeholder']">
										{{ family.relativeType || '请选择亲属关系' }}
									</text>
									<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
								</view>
							</picker>
							<view v-if="isEditMode" class="delete-btn" @click="removeFamilyHistory(index)">
								<uni-icons type="trash" size="18" color="#ff4d4f"></uni-icons>
							</view>
						</view>
						<view class="info-row">
							<text class="info-label">疾病信息：</text>
							<view v-if="!isEditMode" class="info-value-wrapper">
								<text :class="['info-value', (!family.diseases || (Array.isArray(family.diseases) && family.diseases.length === 0)) && 'unknown-value']">
									{{ family.diseases ? formatDiseases(family.diseases) : '未知' }}
								</text>
							</view>
							<input v-else 
								class="edit-input" 
								v-model="editFamilyDiseasesText[index]" 
								placeholder="请输入疾病，多个用逗号分隔"
								@blur="updateFamilyDiseases(index)"
							/>
						</view>
						<view class="disease-divider" v-if="index < (isEditMode ? editData.familyHistory : detailData.familyHistory).length - 1"></view>
					</view>
				</view>
			</view>

			<!-- 遗传病史 -->
			<view class="detail-section" v-if="detailData.geneticHistory || isEditMode">
				<view class="section-title" @click="toggleSection('geneticHistory')">
					<view class="title-left">
						<uni-icons type="heart" size="20" color="#FF9500"></uni-icons>
						<text class="title-text">遗传病史</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('geneticHistory') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('geneticHistory')">
					<view class="info-row">
						<text class="info-label">是否有遗传病史：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'geneticHistory.hasGeneticDisease') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'geneticHistory.hasGeneticDisease'), '无') }}
							</text>
						</view>
						<picker v-else 
							mode="selector" 
							:range="geneticDiseaseOptions" 
							:value="getGeneticDiseaseIndex()"
							@change="onGeneticDiseaseChange($event)"
						>
							<view class="picker-view">
								<text :class="['picker-text', !safeGet(editData, 'geneticHistory.hasGeneticDisease') && 'placeholder']">
									{{ safeGet(editData, 'geneticHistory.hasGeneticDisease') || '请选择' }}
								</text>
								<uni-icons type="arrowdown" size="16" color="#999"></uni-icons>
							</view>
						</picker>
					</view>
					<view class="info-row" v-if="isEditMode || (safeGet(detailData, 'geneticHistory.hasGeneticDisease') && safeGet(detailData, 'geneticHistory.hasGeneticDisease') === '有')">
						<text class="info-label">疾病名称：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'geneticHistory.diseaseName') && 'unknown-value']">
								{{ getFieldValue(safeGet(detailData, 'geneticHistory.diseaseName')) }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editData.geneticHistory.diseaseName" 
							placeholder="请输入疾病名称"
							@blur="updateField('geneticHistory', 'diseaseName', editData.geneticHistory.diseaseName)"
						/>
					</view>
				</view>
			</view>

			<!-- 残疾情况 -->
			<view class="detail-section" v-if="detailData.disabilityHistory || isEditMode">
				<view class="section-title" @click="toggleSection('disabilityHistory')">
					<view class="title-left">
						<uni-icons type="help" size="20" color="#FF9500"></uni-icons>
						<text class="title-text">残疾情况</text>
					</view>
					<view class="collapse-arrow-wrapper">
						<uni-icons 
							:type="isSectionExpanded('disabilityHistory') ? 'arrowup' : 'arrowdown'" 
							size="20" 
							color="#666666"
							class="collapse-arrow"
						></uni-icons>
					</view>
				</view>
				<view class="info-card" v-show="isSectionExpanded('disabilityHistory')">
					<view class="info-row">
						<text class="info-label">残疾类型：</text>
						<view v-if="!isEditMode" class="info-value-wrapper">
							<text :class="['info-value', isNestedFieldEmpty(detailData, 'disabilityHistory.disabilityTypes') && 'unknown-value']">
								{{ formatDisabilityTypes(safeGet(detailData, 'disabilityHistory.disabilityTypes')) || '未知' }}
							</text>
						</view>
						<input v-else 
							class="edit-input" 
							v-model="editDisabilityTypesText" 
							placeholder="请输入残疾类型，多个用逗号分隔"
							@blur="updateDisabilityTypes"
						/>
					</view>
				</view>
			</view>

			<!-- 体格检查 -->
			<view class="detail-section" v-if="detailData.physicalExamination">
				<view class="section-title">
					<uni-icons type="list" size="20" color="#07C160"></uni-icons>
					<text class="title-text">体格检查</text>
				</view>
				<view class="info-card">
					<view class="enum-field" v-for="field in physicalExaminationFields" :key="field.key">
						<view class="field-label">{{ field.label }}：</view>
						<view class="enum-options" v-if="field.options && field.options.length > 0">
							<view class="enum-option" v-for="option in field.options" :key="option.value">
								<checkbox :checked="isOptionSelected(detailData.physicalExamination[field.key], option.value)" color="#07C160" disabled />
								<text class="option-text">{{ option.label }}</text>
							</view>
						</view>
						<view class="field-value" v-else-if="field.isNumber && detailData.physicalExamination[field.key] !== null && detailData.physicalExamination[field.key] !== undefined">
							{{ detailData.physicalExamination[field.key] }}
						</view>
						<view class="field-value" v-if="field.hasTextValue && detailData.physicalExamination[field.textKey]">
							{{ detailData.physicalExamination[field.textKey] }}
						</view>
					</view>
				</view>
			</view>

			<!-- 生活方式状态 -->
			<view class="detail-section" v-if="detailData.lifestyleStatus">
				<view class="section-title">
					<uni-icons type="heart" size="20" color="#07C160"></uni-icons>
					<text class="title-text">生活方式状态</text>
				</view>
				<view class="info-card">
					<view class="enum-field" v-for="field in lifestyleStatusFields" :key="field.key">
						<view class="field-label">{{ field.label }}：</view>
						<view class="enum-options" v-if="field.options && field.options.length > 0">
							<view class="enum-option" v-for="option in field.options" :key="option.value">
								<checkbox :checked="isOptionSelected(detailData.lifestyleStatus[field.key], option.value)" color="#07C160" disabled />
								<text class="option-text">{{ option.label }}</text>
							</view>
						</view>
						<view class="field-value" v-else-if="field.isNumber && detailData.lifestyleStatus[field.key] !== null && detailData.lifestyleStatus[field.key] !== undefined">
							{{ detailData.lifestyleStatus[field.key] }}
						</view>
						<view class="field-value" v-if="field.hasTextValue && detailData.lifestyleStatus[field.textKey]">
							{{ detailData.lifestyleStatus[field.textKey] }}
						</view>
					</view>
				</view>
			</view>

			<!-- 系统疾病与症状筛查 -->
			<view class="detail-section" v-if="detailData.systemDiseaseScreening">
				<view class="section-title">
					<uni-icons type="medal" size="20" color="#07C160"></uni-icons>
					<text class="title-text">系统疾病与症状筛查</text>
				</view>
				<view class="info-card">
					<view class="enum-field" v-for="field in systemScreeningFields" :key="field.key">
						<view class="field-label">{{ field.label }}：</view>
						<view class="enum-options">
							<view class="enum-option" v-for="option in field.options" :key="option.value">
								<checkbox :checked="isOptionSelected(detailData.systemDiseaseScreening[field.key], option.value)" color="#07C160" disabled />
								<text class="option-text">{{ option.label }}</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<!-- 心理评估 -->
			<view class="detail-section" v-if="detailData.psychologicalAssessment">
				<view class="section-title">
					<uni-icons type="chatbubble" size="20" color="#07C160"></uni-icons>
					<text class="title-text">心理评估</text>
				</view>
				<view class="info-card">
					<view class="enum-field" v-for="field in psychologicalFields" :key="field.key">
						<view class="field-label">{{ field.label }}：</view>
						<view class="enum-options">
							<view class="enum-option" v-for="option in field.options" :key="option.value">
								<checkbox :checked="isOptionSelected(detailData.psychologicalAssessment[field.key], option.value)" color="#07C160" disabled />
								<text class="option-text">{{ option.label }}</text>
							</view>
						</view>
						<view class="field-value" v-if="field.hasTextValue && detailData.psychologicalAssessment[field.textKey]">
							{{ detailData.psychologicalAssessment[field.textKey] }}
						</view>
					</view>
				</view>
			</view>

			<!-- 社会关系评估 -->
			<view class="detail-section" v-if="detailData.socialRelationshipAssessment">
				<view class="section-title">
					<uni-icons type="person-filled" size="20" color="#07C160"></uni-icons>
					<text class="title-text">社会关系评估</text>
				</view>
				<view class="info-card">
					<view class="enum-field" v-for="field in socialAssessmentFields" :key="field.key">
						<view class="field-label">{{ field.label }}：</view>
						<view class="enum-options">
							<view class="enum-option" v-for="option in field.options" :key="option.value">
								<checkbox :checked="isOptionSelected(detailData.socialRelationshipAssessment[field.key], option.value)" color="#07C160" disabled />
								<text class="option-text">{{ option.label }}</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<!-- 健康总结 -->
			<view class="detail-section" v-if="detailData.healthSummary">
				<view class="section-title">
					<uni-icons type="compose" size="20" color="#07C160"></uni-icons>
					<text class="title-text">AI健康总结</text>
				</view>
				<view class="info-card">
					<view class="info-row" v-if="detailData.healthSummary.overallHealthScore !== null">
						<text class="info-label">健康评分：</text>
						<text class="info-value health-score">{{ detailData.healthSummary.overallHealthScore }}分</text>
					</view>
				<view class="info-row" v-if="detailData.healthSummary.riskLevel">
					<text class="info-label">风险等级：</text>
					<text class="info-value" :class="riskLevelClass">{{ detailData.healthSummary.riskLevel }}</text>
				</view>
					<view class="info-row" v-if="detailData.healthSummary.analysisSummary">
						<text class="info-label">分析摘要：</text>
						<text class="info-value summary-text">{{ detailData.healthSummary.analysisSummary }}</text>
					</view>
					<view class="info-row" v-if="detailData.healthSummary.createTime">
						<text class="info-label">分析时间：</text>
						<text class="info-value">{{ formatDateTime(detailData.healthSummary.createTime) }}</text>
					</view>
				</view>
			</view>

			<!-- 健康建议 -->
			<view class="detail-section" v-if="detailData.healthRecommendations && detailData.healthRecommendations.length > 0">
				<view class="section-title">
					<uni-icons type="chatboxes" size="20" color="#07C160"></uni-icons>
					<text class="title-text">健康建议</text>
				</view>
				<view class="recommendation-list">
					<view class="recommendation-item" v-for="(recommendation, index) in detailData.healthRecommendations" :key="recommendation.recommendationId">
						<view class="recommendation-header">
							<view class="recommendation-priority" :class="recommendation.priority === '低' ? 'priority-low' : (recommendation.priority === '高' ? 'priority-high' : 'priority-medium')">
								{{ recommendation.priority || '中' }}
							</view>
							<text class="recommendation-type">{{ getRecommendationTypeText(recommendation.recommendationType) }}</text>
						</view>
						<view class="recommendation-title">{{ recommendation.title }}</view>
						<view class="recommendation-content">{{ recommendation.content }}</view>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import config from '@/utils/config.js'

	export default {
		data() {
			return {
				loading: false,
				archiveId: null,
				detailData: {},
				isEditMode: false, // 编辑模式
				editData: {}, // 编辑中的数据副本
				// 各个section的展开/收起状态（默认全部收起）
				sectionExpanded: {
					healthArchive: false,
					healthProfileTags: false,
					userInfo: false,
					userEmergencyContacts: false,
					userCertificates: false,
					allergyHistory: false,
					exposureHistory: false,
					diseaseHistory: false,
					vaccinationHistory: false,
					familyHistory: false,
					geneticHistory: false,
					disabilityHistory: false
				},
				// 选择器选项
				pregnancyRiskOptions: ['低风险', '一般风险', '较高风险', '高风险'],
				weightStatusOptions: ['低', '正常', '超重', '肥胖'],
				bloodTypeOptions: ['A', 'B', 'O', 'AB', '不详'],
				editChronicDiseaseText: '', // 编辑时的慢性病文本
				editStatutoryInfoText: '', // 编辑时的法定传染病文本
				editDrugAllergyText: '', // 编辑时的药物过敏文本
				editFamilyDiseasesText: [], // 编辑时的家族史疾病文本数组
				editDisabilityTypesText: '', // 编辑时的残疾类型文本
				// 过敏史和暴露史选择器选项
				allergyTypeOptions: ['无', '药物过敏', '食物过敏', '其他过敏'],
				exposureTypeOptions: ['无', '化学品', '毒物', '射线'],
				// 家族史和遗传病史选择器选项
				relativeTypeOptions: ['父亲', '母亲', '兄弟姐妹', '子女', '祖父', '祖母', '外祖父', '外祖母', '其他'],
				geneticDiseaseOptions: ['无', '有'],
				// 用户基本信息选择器选项
				genderOptions: ['男', '女', '未说明的性别', '未知的性别'],
				residenceTypeOptions: ['户籍', '非户籍'],
				educationLevelOptions: ['研究生', '大学本科', '大学专科和专科学校', '中等专业学校', '技工学校', '高中', '初中', '小学', '文盲或半文盲', '不详'],
				occupationOptions: ['党的机关、国家机关、群众团体和社会组织、企事业单位负责人', '专业技术人员', '办事人员和有关人员', '社会生产服务和生活服务人员', '农、林、牧、渔业生产及辅助人员', '生产制造及有关人员', '军队人员', '不便分类的其他从业人员', '无职业', '学生'],
				maritalStatusOptions: ['未婚', '已婚', '丧偶', '离婚', '未说明的婚姻状况'],
				paymentMethodOptions: ['城镇职工基本医疗保险', '城乡居民基本医疗保险', '医疗救助', '商业医疗保险', '公费', '自费', '其他'],
				// 生活方式状态字段定义
				lifestyleStatusFields: [
					{ key: 'dietType', label: '主要膳食种类', options: [
						{ value: '0-素食', label: '素食' },
						{ value: '1-中餐', label: '中餐' },
						{ value: '2-海鲜', label: '海鲜' },
						{ value: '3-西餐', label: '西餐' },
						{ value: '4-印度菜', label: '印度菜' },
						{ value: '5-日韩料理', label: '日韩料理' },
						{ value: '6-即食品', label: '即食品' },
						{ value: '7-其他', label: '其他' }
					]},
					{ key: 'mealsRegular', label: '三餐是否规律', options: [
						{ value: true, label: '是' },
						{ value: false, label: '否' }
					], hasTextValue: true, textKey: 'mealsIrregularDesc' },
					{ key: 'eatOutFrequency', label: '外出用餐频率（次/周）', isNumber: true },
					{ key: 'specialDietHabit', label: '特殊饮食习惯', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'specialDietDesc' },
					{ key: 'appetite', label: '食欲', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-亢进', label: '亢进' },
						{ value: '2-下降', label: '下降' },
						{ value: '3-厌食', label: '厌食' }
					]},
					{ key: 'urination', label: '排尿情况', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-少尿', label: '少尿' },
						{ value: '2-多尿', label: '多尿' },
						{ value: '3-无尿', label: '无尿' },
						{ value: '4-膀胱刺激征', label: '膀胱刺激征' },
						{ value: '5-尿潴留', label: '尿潴留' },
						{ value: '6-尿失禁', label: '尿失禁' }
					]},
					{ key: 'defecationStatus', label: '排便情况', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-便秘', label: '便秘' },
						{ value: '2-腹泻', label: '腹泻' }
					]},
					{ key: 'constipationDays', label: '便秘持续天数', isNumber: true },
					{ key: 'constipationNeedAssist', label: '是否辅助排便', options: [
						{ value: true, label: '是' },
						{ value: false, label: '否' }
					]},
					{ key: 'diarrheaTimesPerDay', label: '腹泻次数（次/日）', isNumber: true },
					{ key: 'activityAbility', label: '活动能力', options: [
						{ value: '0-无限制', label: '无限制' },
						{ value: '1-需使用工具', label: '需使用工具' },
						{ value: '2-床旁活动', label: '床旁活动' },
						{ value: '3-卧床', label: '卧床' }
					]},
					{ key: 'selfCareAbility', label: '自理能力', options: [
						{ value: '0-完全自理', label: '完全自理' },
						{ value: '1-半自理', label: '半自理' },
						{ value: '2-失能', label: '失能' }
					]},
					{ key: 'exerciseType', label: '体格锻炼方式', options: [
						{ value: '0-健身房', label: '健身房' },
						{ value: '1-户外慢跑', label: '户外慢跑' },
						{ value: '2-户外散步', label: '户外散步' },
						{ value: '3-游泳', label: '游泳' }
					]},
					{ key: 'exerciseFrequency', label: '体格锻炼频率（次/周）', isNumber: true },
					{ key: 'commuteMode', label: '外出/上班方式', options: [
						{ value: '0-步行', label: '步行' },
						{ value: '1-骑自行车', label: '骑自行车' },
						{ value: '2-代步平衡车', label: '代步平衡车' },
						{ value: '3-汽车', label: '汽车' }
					]},
					{ key: 'routineRegular', label: '作息是否规律', options: [
						{ value: true, label: '是' },
						{ value: false, label: '否' }
					], hasTextValue: true, textKey: 'routineIrregularDesc' },
					{ key: 'sleepStatus', label: '睡眠情况', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'sleepDesc' },
					{ key: 'regularTherapy', label: '定期保养/理疗', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'therapyProject' },
					{ key: 'therapyFrequencyPerYear', label: '理疗频率（次/年）', isNumber: true },
					{ key: 'regularCheckup', label: '是否定期体检', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'checkupFrequencyPerYear', label: '体检频率（次/年）', isNumber: true },
					{ key: 'weightControl', label: '减肥/增重行为', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'weightChange', label: '体重与去年对比', options: [
						{ value: '0-基本无差异', label: '基本无差异' },
						{ value: '1-有差异（2-5斤）', label: '有差异（2-5斤）' },
						{ value: '2-差异较大（5斤以上）', label: '差异较大（5斤以上）' }
					]},
					{ key: 'smokingStatus', label: '吸烟情况', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-偶吸', label: '偶吸' },
						{ value: '2-大量', label: '大量' }
					]},
					{ key: 'cigarettesPerDay', label: '吸烟量（支/日）', isNumber: true },
					{ key: 'smokingYears', label: '已吸烟年数', isNumber: true },
					{ key: 'quitSmokingYears', label: '已戒烟年数', isNumber: true },
					{ key: 'drinkingStatus', label: '饮酒情况', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-偶饮', label: '偶饮' },
						{ value: '2-大量', label: '大量' }
					]},
					{ key: 'drinkingTimesPerDay', label: '饮酒次数（次/日）', isNumber: true },
					{ key: 'drinkingYears', label: '已饮酒年数', isNumber: true },
					{ key: 'quitDrinkingYears', label: '已戒酒年数', isNumber: true },
					{ key: 'drugDependence', label: '药物依赖', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'drugDependenceDesc' }
				],
				// 系统疾病与症状筛查字段定义
				systemScreeningFields: [
					{ key: 'headFacialSymptoms', label: '头颅五官系统症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-视力障碍', label: '视力障碍' },
						{ value: '2-眼干', label: '眼干' },
						{ value: '3-耳聋', label: '耳聋' },
						{ value: '4-耳鸣', label: '耳鸣' },
						{ value: '5-眩晕', label: '眩晕' },
						{ value: '6-鼻出血', label: '鼻出血' },
						{ value: '7-牙痛', label: '牙痛' },
						{ value: '8-牙龈出血', label: '牙龈出血' },
						{ value: '9-声嘶', label: '声嘶' },
						{ value: '10-口角歪斜', label: '口角歪斜' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'respiratorySymptoms', label: '呼吸系统症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-咳嗽', label: '咳嗽' },
						{ value: '2-咳痰', label: '咳痰' },
						{ value: '3-咯血', label: '咯血' },
						{ value: '4-呼吸困难', label: '呼吸困难' },
						{ value: '5-喘息', label: '喘息' },
						{ value: '6-发热', label: '发热' },
						{ value: '7-盗汗', label: '盗汗' },
						{ value: '8-胸闷', label: '胸闷' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'circulatorySymptoms', label: '循环系统症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-心悸', label: '心悸' },
						{ value: '2-活动后气促', label: '活动后气促' },
						{ value: '3-心前区疼痛', label: '心前区疼痛' },
						{ value: '4-下肢水肿', label: '下肢水肿' },
						{ value: '5-晕厥', label: '晕厥' },
						{ value: '6-高血压', label: '高血压' },
						{ value: '7-高血脂', label: '高血脂' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'digestiveSymptoms', label: '消化系统症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-食欲减退', label: '食欲减退' },
						{ value: '2-反酸', label: '反酸' },
						{ value: '3-嗳气', label: '嗳气' },
						{ value: '4-恶心', label: '恶心' },
						{ value: '5-呕吐', label: '呕吐' },
						{ value: '6-吞咽困难', label: '吞咽困难' },
						{ value: '7-腹胀', label: '腹胀' },
						{ value: '8-腹痛', label: '腹痛' },
						{ value: '9-腹泻', label: '腹泻' },
						{ value: '10-便秘', label: '便秘' },
						{ value: '11-呕血', label: '呕血' },
						{ value: '12-黑便', label: '黑便' },
						{ value: '13-溃疡', label: '溃疡' },
						{ value: '14-炎症', label: '炎症' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'urogenitalSymptoms', label: '泌尿生殖系统症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-尿频', label: '尿频' },
						{ value: '2-尿急', label: '尿急' },
						{ value: '3-尿痛', label: '尿痛' },
						{ value: '4-排尿困难', label: '排尿困难' },
						{ value: '5-尿量异常', label: '尿量异常' },
						{ value: '6-血尿', label: '血尿' },
						{ value: '7-尿色改变', label: '尿色改变' },
						{ value: '8-尿失禁', label: '尿失禁' },
						{ value: '9-颜面水肿', label: '颜面水肿' },
						{ value: '10-泌尿生殖系统炎症', label: '泌尿生殖系统炎症' },
						{ value: '11-痛经', label: '痛经' },
						{ value: '12-子宫肌瘤', label: '子宫肌瘤' },
						{ value: '13-前列腺增生', label: '前列腺增生' },
						{ value: '14-性欲低下', label: '性欲低下' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'endocrineMetabolicSymptoms', label: '内分泌与代谢症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-食欲亢进', label: '食欲亢进' },
						{ value: '2-畏寒', label: '畏寒' },
						{ value: '3-怕热', label: '怕热' },
						{ value: '4-多汗', label: '多汗' },
						{ value: '5-烦渴', label: '烦渴' },
						{ value: '6-多尿', label: '多尿' },
						{ value: '7-高尿酸', label: '高尿酸' },
						{ value: '8-双手震颤', label: '双手震颤' },
						{ value: '9-体重改变', label: '体重改变' },
						{ value: '10-毛发增多或脱落', label: '毛发增多或脱落' },
						{ value: '11-色素沉着', label: '色素沉着' },
						{ value: '12-性功能改变', label: '性功能改变' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'hematopoieticSymptoms', label: '造血系统症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-乏力', label: '乏力' },
						{ value: '2-头晕', label: '头晕' },
						{ value: '3-眼花', label: '眼花' },
						{ value: '4-黄疸', label: '黄疸' },
						{ value: '5-皮肤黏膜苍白', label: '皮肤黏膜苍白' },
						{ value: '6-皮肤黏膜出血', label: '皮肤黏膜出血' },
						{ value: '7-鼻出血', label: '鼻出血' },
						{ value: '8-淋巴结或肝脾大', label: '淋巴结或肝脾大' },
						{ value: '9-血液性骨痛', label: '血液性骨痛' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'musculoskeletalSymptoms', label: '肌肉骨骼系统症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-关节疼痛', label: '关节疼痛' },
						{ value: '2-关节红肿', label: '关节红肿' },
						{ value: '3-关节畸形', label: '关节畸形' },
						{ value: '4-脊柱畸形', label: '脊柱畸形' },
						{ value: '5-肢体活动障碍', label: '肢体活动障碍' },
						{ value: '6-肌无力', label: '肌无力' },
						{ value: '7-肌肉萎缩', label: '肌肉萎缩' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'neurologicalSymptoms', label: '神经系统症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-头痛', label: '头痛' },
						{ value: '2-头晕', label: '头晕' },
						{ value: '3-晕厥', label: '晕厥' },
						{ value: '4-失眠', label: '失眠' },
						{ value: '5-意识障碍', label: '意识障碍' },
						{ value: '6-抽搐', label: '抽搐' },
						{ value: '7-瘫痪', label: '瘫痪' },
						{ value: '8-皮肤感觉异常', label: '皮肤感觉异常' },
						{ value: '9-记忆力减退', label: '记忆力减退' },
						{ value: '10-语言障碍', label: '语言障碍' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'mentalStateSymptoms', label: '精神状态症状', options: [
						{ value: '0-正常/无异', label: '正常/无异' },
						{ value: '1-情绪改变', label: '情绪改变' },
						{ value: '2-疲劳', label: '疲劳' },
						{ value: '3-压抑', label: '压抑' },
						{ value: '4-焦虑', label: '焦虑' },
						{ value: '5-抑郁', label: '抑郁' },
						{ value: '6-幻觉', label: '幻觉' },
						{ value: '7-妄想', label: '妄想' },
						{ value: '8-定向力障碍', label: '定向力障碍' }
					]}
				],
				// 心理评估字段定义
				psychologicalFields: [
					{ key: 'fatigueDepression', label: '疲劳、压抑', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'memoryDecline', label: '记忆力减退', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'adaptabilityDecline', label: '适应能力减退', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'vitalityResponseDecline', label: '活力、反应能力减退', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'emotionStatus', label: '情绪状态', options: [
						{ value: '0-镇静', label: '镇静' },
						{ value: '1-易激动', label: '易激动' },
						{ value: '2-焦虑', label: '焦虑' },
						{ value: '3-恐惧', label: '恐惧' },
						{ value: '4-紧张', label: '紧张' },
						{ value: '5-悲哀', label: '悲哀' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'stressStatus', label: '是否存在压力', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'stressSource', label: '压力来源', options: [
						{ value: '1-工作', label: '工作' },
						{ value: '2-家庭', label: '家庭' },
						{ value: '3-社会', label: '社会' }
					]},
					{ key: 'stressReliefMethods', label: '缓压方法', options: [
						{ value: '1-睡眠', label: '睡眠' },
						{ value: '2-运动', label: '运动' },
						{ value: '3-旅游', label: '旅游' },
						{ value: '4-音乐', label: '音乐' },
						{ value: '5-娱乐', label: '娱乐' },
						{ value: '6-心理咨询师', label: '心理咨询师' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'selfPerception', label: '对自我的看法', options: [
						{ value: '0-满意', label: '满意' },
						{ value: '1-不满意', label: '不满意' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'diseaseCognition', label: '对疾病的认识程度', options: [
						{ value: '0-完全认识', label: '完全认识' },
						{ value: '1-部分认识', label: '部分认识' },
						{ value: '2-不认识', label: '不认识' }
					]},
					{ key: 'majorLifeEvent', label: '过去1年内是否有重要生活事件', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'majorLifeEventDesc' },
					{ key: 'preferredConfidant', label: '遇到困难最愿倾诉对象', options: [
						{ value: '1-父母', label: '父母' },
						{ value: '2-子女', label: '子女' },
						{ value: '99-其他', label: '其他' }
					]}
				],
				// 社会关系评估字段定义
				socialAssessmentFields: [
					{ key: 'familyRelationship', label: '家庭关系', options: [
						{ value: '0-和睦', label: '和睦' },
						{ value: '1-冷淡', label: '冷淡' },
						{ value: '2-紧张', label: '紧张' }
					]},
					{ key: 'maritalStatus', label: '婚姻状况', options: [
						{ value: '0-未婚', label: '未婚' },
						{ value: '1-已婚', label: '已婚' },
						{ value: '2-离婚', label: '离婚' },
						{ value: '3-丧偶', label: '丧偶' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'livingCondition', label: '居住情况', options: [
						{ value: '0-独居', label: '独居' },
						{ value: '1-和家人同住', label: '和家人同住' },
						{ value: '2-和亲友同住', label: '和亲友同住' },
						{ value: '3-酒店', label: '酒店' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'occupationType', label: '职业性质', options: [
						{ value: '0-国家机关负责人', label: '国家机关负责人' },
						{ value: '1-企业事业负责人', label: '企业事业负责人' },
						{ value: '2-商业服务业人员', label: '商业服务业人员' },
						{ value: '3-专业技术人员', label: '专业技术人员' },
						{ value: '4-军人', label: '军人' },
						{ value: '5-离职', label: '离职' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'educationLevel', label: '文化程度', options: [
						{ value: '0-小学或初中', label: '小学或初中' },
						{ value: '1-高中或中专', label: '高中或中专' },
						{ value: '2-大专', label: '大专' },
						{ value: '3-本科', label: '本科' },
						{ value: '4-硕士', label: '硕士' },
						{ value: '5-硕士以上', label: '硕士以上' }
					]},
					{ key: 'socialInteraction', label: '社会交往情况', options: [
						{ value: '0-频繁', label: '频繁' },
						{ value: '1-正常', label: '正常' },
						{ value: '2-较少', label: '较少' },
						{ value: '3-回避', label: '回避' }
					]},
					{ key: 'medicalPaymentMethod', label: '医疗费用支付形式', options: [
						{ value: '0-公费', label: '公费' },
						{ value: '1-医疗保险', label: '医疗保险' },
						{ value: '2-自费', label: '自费' },
						{ value: '99-其他', label: '其他' }
					]}
				],
				// 体格检查字段定义（部分主要字段，其他字段类似处理）
				physicalExaminationFields: [
					{ key: 'temperature', label: '体温（℃）', isNumber: true },
					{ key: 'pulse', label: '脉搏（次/分）', isNumber: true },
					{ key: 'respiration', label: '呼吸（次/分）', isNumber: true },
					{ key: 'systolicBp', label: '收缩压（mmHg）', isNumber: true },
					{ key: 'diastolicBp', label: '舒张压（mmHg）', isNumber: true },
					{ key: 'heightCm', label: '身高（cm）', isNumber: true },
					{ key: 'weightKg', label: '体重（kg）', isNumber: true },
					{ key: 'gluValue', label: '血糖（mmol/L）', isNumber: true },
					{ key: 'gluType', label: '血糖类型', options: [
						{ value: '0-空腹', label: '空腹' },
						{ value: '1-餐后', label: '餐后' }
					]},
					{ key: 'development', label: '发育情况', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'developmentDesc' },
					{ key: 'nutrition', label: '营养状况', options: [
						{ value: '0-良好', label: '良好' },
						{ value: '1-中等', label: '中等' },
						{ value: '2-不良', label: '不良' }
					]},
					{ key: 'bodyType', label: '体型', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-肥胖', label: '肥胖' },
						{ value: '2-消瘦', label: '消瘦' }
					]},
					{ key: 'facialExpression', label: '面容', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-病容', label: '病容' }
					], hasTextValue: true, textKey: 'facialExpressionDesc' },
					{ key: 'posture', label: '体位', options: [
						{ value: '0-主动', label: '主动' },
						{ value: '1-被动', label: '被动' },
						{ value: '2-强迫体位', label: '强迫体位' }
					], hasTextValue: true, textKey: 'postureDesc' },
					{ key: 'gait', label: '步态', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'gaitDesc' },
					{ key: 'consciousness', label: '意识状态', options: [
						{ value: '0-清楚', label: '清楚' },
						{ value: '1-嗜睡', label: '嗜睡' },
						{ value: '2-意识模糊', label: '意识模糊' },
						{ value: '3-昏睡', label: '昏睡' },
						{ value: '4-浅昏迷', label: '浅昏迷' },
						{ value: '5-深昏迷', label: '深昏迷' }
					]},
					{ key: 'speech', label: '语言表达', options: [
						{ value: '0-清楚', label: '清楚' },
						{ value: '1-含糊', label: '含糊' },
						{ value: '2-语言困难', label: '语言困难' },
						{ value: '3-失语', label: '失语' }
					]},
					{ key: 'skinColor', label: '皮肤颜色', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-发红', label: '发红' },
						{ value: '2-苍白', label: '苍白' },
						{ value: '3-发绀', label: '发绀' },
						{ value: '4-黄染', label: '黄染' },
						{ value: '5-色素沉着', label: '色素沉着' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'skinMoisture', label: '皮肤湿度', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-潮湿', label: '潮湿' },
						{ value: '2-干燥', label: '干燥' }
					]},
					{ key: 'skinTemperature', label: '皮肤温度', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-稍热', label: '稍热' },
						{ value: '2-稍冷', label: '稍冷' }
					]},
					{ key: 'skinElasticity', label: '皮肤弹性', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-减退', label: '减退' }
					]},
					{ key: 'edema', label: '水肿', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'edemaDesc' },
					{ key: 'skinIntegrity', label: '皮肤完整性', options: [
						{ value: '0-完整', label: '完整' },
						{ value: '1-皮疹', label: '皮疹' },
						{ value: '2-破损', label: '破损' },
						{ value: '3-压疮', label: '压疮' },
						{ value: '99-其他', label: '其他' }
					], hasTextValue: true, textKey: 'skinIntegrityDesc' },
					{ key: 'lymphNodes', label: '淋巴结', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-肿大', label: '肿大' }
					]},
					{ key: 'eyelid', label: '眼睑', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-水肿', label: '水肿' }
					]},
					{ key: 'conjunctiva', label: '结膜', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-水肿', label: '水肿' },
						{ value: '2-出血', label: '出血' }
					]},
					{ key: 'sclera', label: '巩膜', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-黄染', label: '黄染' }
					]},
					{ key: 'pupil', label: '瞳孔', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'pupilDesc' },
					{ key: 'lightReflex', label: '对光反射', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-迟钝', label: '迟钝' },
						{ value: '2-消失', label: '消失' }
					]},
					{ key: 'lips', label: '口唇', options: [
						{ value: '0-红润', label: '红润' },
						{ value: '1-发绀', label: '发绀' },
						{ value: '2-红肿', label: '红肿' },
						{ value: '3-苍白', label: '苍白' },
						{ value: '4-疱疹', label: '疱疹' },
						{ value: '5-歪斜', label: '歪斜' }
					]},
					{ key: 'oralMucosa', label: '口腔黏膜', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-充血', label: '充血' },
						{ value: '2-出血点', label: '出血点' },
						{ value: '3-糜烂溃疡', label: '糜烂溃疡' },
						{ value: '4-疱疹', label: '疱疹' },
						{ value: '5-白斑', label: '白斑' },
						{ value: '99-其他', label: '其他' }
					]},
					{ key: 'teeth', label: '牙齿', options: [
						{ value: '0-完好', label: '完好' },
						{ value: '1-缺齿', label: '缺齿' },
						{ value: '2-龋齿', label: '龋齿' },
						{ value: '3-义齿', label: '义齿' }
					]},
					{ key: 'vision', label: '视力', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'visionDesc' },
					{ key: 'hearing', label: '听力', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'hearingDesc' },
					{ key: 'smell', label: '嗅觉', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'smellDesc' },
					{ key: 'neckStiffness', label: '颈项强直', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'jugularVein', label: '颈静脉', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-怒张', label: '怒张' }
					]},
					{ key: 'trachea', label: '气管', options: [
						{ value: '0-居中', label: '居中' },
						{ value: '1-偏移', label: '偏移' }
					]},
					{ key: 'hepatojugularReflex', label: '肝颈静脉回流征', options: [
						{ value: '0-阴性', label: '阴性' },
						{ value: '1-阳性', label: '阳性' }
					]},
					{ key: 'breathingMode', label: '呼吸方式', options: [
						{ value: '0-自主呼吸', label: '自主呼吸' },
						{ value: '1-机械呼吸', label: '机械呼吸' }
					]},
					{ key: 'breathingRhythm', label: '呼吸节律', options: [
						{ value: '0-规则', label: '规则' },
						{ value: '1-不规则', label: '不规则' }
					], hasTextValue: true, textKey: 'breathingRhythmDesc' },
					{ key: 'dyspnea', label: '呼吸困难', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-轻度', label: '轻度' },
						{ value: '2-中度', label: '中度' },
						{ value: '3-重度', label: '重度' },
						{ value: '4-极重度', label: '极重度' }
					]},
					{ key: 'breathSound', label: '呼吸音', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'breathSoundDesc' },
					{ key: 'rales', label: '啰音', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'ralesDesc' },
					{ key: 'heartRate', label: '心率（次/分）', isNumber: true },
					{ key: 'heartRhythm', label: '心律', options: [
						{ value: '0-齐', label: '齐' },
						{ value: '1-不齐', label: '不齐' }
					]},
					{ key: 'murmur', label: '杂音', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'murmurDesc' },
					{ key: 'abdomenShape', label: '腹部外形', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-膨隆', label: '膨隆' },
						{ value: '2-凹陷', label: '凹陷' },
						{ value: '3-胃型', label: '胃型' },
						{ value: '4-肠型', label: '肠型' }
					]},
					{ key: 'abdominalMass', label: '腹部包块', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'abdominalMassDesc' },
					{ key: 'abdominalTension', label: '腹肌紧张', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'abdominalTensionDesc' },
					{ key: 'tenderness', label: '压痛', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'tendernessDesc' },
					{ key: 'reboundTenderness', label: '反跳痛', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'reboundTendernessDesc' },
					{ key: 'hepatomegaly', label: '肝大', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'hepatomegalyDesc' },
					{ key: 'splenomegaly', label: '脾大', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'splenomegalyDesc' },
					{ key: 'shiftingDullness', label: '移动性浊音', options: [
						{ value: '0-阴性', label: '阴性' },
						{ value: '1-阳性', label: '阳性' }
					]},
					{ key: 'bowelSounds', label: '肠鸣音（次/分）', isNumber: true },
					{ key: 'bowelSoundStatus', label: '肠鸣音状态', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-亢进', label: '亢进' },
						{ value: '2-减弱', label: '减弱' },
						{ value: '3-消失', label: '消失' }
					]},
					{ key: 'rectalExam', label: '直肠肛门', options: [
						{ value: '0-未查', label: '未查' },
						{ value: '1-正常', label: '正常' },
						{ value: '2-异常', label: '异常' }
					], hasTextValue: true, textKey: 'rectalExamDesc' },
					{ key: 'genitalExam', label: '外生殖器', options: [
						{ value: '0-未查', label: '未查' },
						{ value: '1-正常', label: '正常' },
						{ value: '2-异常', label: '异常' }
					], hasTextValue: true, textKey: 'genitalExamDesc' },
					{ key: 'spineShape', label: '脊柱外形', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-畸形', label: '畸形' }
					], hasTextValue: true, textKey: 'spineDesc' },
					{ key: 'spineActivity', label: '脊柱活动', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-受限', label: '受限' }
					]},
					{ key: 'limbShape', label: '四肢外形', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-畸形', label: '畸形' }
					], hasTextValue: true, textKey: 'limbDesc' },
					{ key: 'limbActivity', label: '四肢活动', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-受限', label: '受限' }
					]},
					{ key: 'pain', label: '疼痛', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'painDesc' },
					{ key: 'painScore', label: '疼痛评分', options: [
						{ value: '0-无痛', label: '无痛' },
						{ value: '1-1至3分', label: '1至3分' },
						{ value: '2-4至6分', label: '4至6分' },
						{ value: '3-9分', label: '9分' },
						{ value: '4-10分', label: '10分' }
					]},
					{ key: 'muscleStrength', label: '肌力', options: [
						{ value: '0-正常', label: '正常' },
						{ value: '1-异常', label: '异常' }
					], hasTextValue: true, textKey: 'muscleStrengthDesc' },
					{ key: 'paralysis', label: '肢体瘫痪', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					], hasTextValue: true, textKey: 'paralysisDesc' },
					{ key: 'muscleStrengthGrade', label: '肌力分级', isNumber: true },
					{ key: 'pathologicalReflex', label: '病理反射', options: [
						{ value: '0-阴性', label: '阴性' },
						{ value: '1-阳性', label: '阳性' }
					]},
					{ key: 'meningealSign', label: '脑膜刺激征', options: [
						{ value: '0-无', label: '无' },
						{ value: '1-有', label: '有' }
					]},
					{ key: 'meningealSignType', label: '脑膜刺激征类型', options: [
						{ value: '0-颈强直', label: '颈强直' },
						{ value: '1-Kerning征', label: 'Kerning征' },
						{ value: '2-Brudzinski征', label: 'Brudzinski征' }
					]}
				]
			}
		},
		computed: {
			chronicDiseaseList() {
				if (!this.detailData.healthProfileTags || !this.detailData.healthProfileTags.chronicDisease) {
					return []
				}
				try {
					return JSON.parse(this.detailData.healthProfileTags.chronicDisease)
				} catch (e) {
					return []
				}
			},
			statutoryInfoList() {
				if (!this.detailData.healthProfileTags || !this.detailData.healthProfileTags.statutoryInfo) {
					return []
				}
				try {
					return JSON.parse(this.detailData.healthProfileTags.statutoryInfo)
				} catch (e) {
					return []
				}
			},
			// 风险等级样式类
			riskLevelClass() {
				if (!this.detailData.healthSummary || !this.detailData.healthSummary.riskLevel) {
					return ''
				}
				const level = this.detailData.healthSummary.riskLevel
				if (level === '低') return 'risk-low'
				if (level === '中') return 'risk-medium'
				if (level === '高') return 'risk-high'
				if (level === '极高') return 'risk-very-high'
				return ''
			}
		},
		onLoad(options) {
			if (options.archiveId) {
				this.archiveId = parseInt(options.archiveId)
				this.loadArchiveDetail()
			} else {
				uni.showToast({
					title: '档案ID不能为空',
					icon: 'none'
				})
				setTimeout(() => {
					uni.navigateBack()
				}, 1500)
			}
		},
		methods: {
			// 判断选项是否被选中
			// fieldValue可能是字符串（SET类型，逗号分隔）或单个值（ENUM类型）或布尔值
			isOptionSelected(fieldValue, optionValue) {
				if (fieldValue === null || fieldValue === undefined) {
					return false
				}
				// 如果是布尔值
				if (typeof fieldValue === 'boolean') {
					return fieldValue === optionValue
				}
				// 如果是字符串
				if (typeof fieldValue === 'string') {
					// SET类型：逗号分隔的多个值，如 "0-正常,1-异常"
					if (fieldValue.includes(',')) {
						const values = fieldValue.split(',').map(v => v.trim())
						return values.includes(optionValue)
					} else {
						// ENUM类型：单个值
						return fieldValue === optionValue
					}
				}
				return false
			},
			loadArchiveDetail() {
				const loginStatus = uni.getStorageSync('isLogin')
				if (!loginStatus) {
					uni.showToast({
						title: '请先登录',
						icon: 'none'
					})
					setTimeout(() => {
						uni.navigateTo({
							url: '/pages/login/login'
						})
					}, 1500)
					return
				}

				this.loading = true
				const token = uni.getStorageSync('token')
				const userInfo = uni.getStorageSync('userInfo')
				const userId = userInfo ? userInfo.userId : null

				uni.request({
					url: `${config.baseUrl}/health-archive-process/detail/${this.archiveId}`,
					method: 'GET',
					header: {
						'userId': userId || '',
						'token': token || ''
					},
					success: (res) => {
						this.loading = false
						if (res.statusCode === 200 && res.data.code === 200) {
							this.detailData = res.data.data || {}
							// 初始化编辑数据副本
							this.initEditData()
						} else {
							uni.showToast({
								title: res.data.message || '查询失败',
								icon: 'none'
							})
							setTimeout(() => {
								uni.navigateBack()
							}, 1500)
						}
					},
					fail: (err) => {
						this.loading = false
						console.error('查询健康档案详情失败', err)
						uni.showToast({
							title: '网络错误，请稍后重试',
							icon: 'none'
						})
						setTimeout(() => {
							uni.navigateBack()
						}, 1500)
					}
				})
			},
			// 格式化日期时间
			formatDateTime(dateTimeStr) {
				if (!dateTimeStr) return ''
				const date = new Date(dateTimeStr)
				const year = date.getFullYear()
				const month = String(date.getMonth() + 1).padStart(2, '0')
				const day = String(date.getDate()).padStart(2, '0')
				const hours = String(date.getHours()).padStart(2, '0')
				const minutes = String(date.getMinutes()).padStart(2, '0')
				return `${year}-${month}-${day} ${hours}:${minutes}`
			},
			// 格式化药物过敏详情
			formatDrugAllergy(drugAllergyDetails) {
				if (!drugAllergyDetails) return ''
				try {
					const details = typeof drugAllergyDetails === 'string' ? JSON.parse(drugAllergyDetails) : drugAllergyDetails
					const drugNames = {
						'penicillin': '青霉素',
						'sulfa': '磺胺类',
						'streptomycin': '链霉素',
						'others': '其他'
					}
					const selectedDrugs = []
					for (const [key, value] of Object.entries(details)) {
						if (value === true && key !== 'others') {
							selectedDrugs.push(drugNames[key] || key)
						}
					}
					if (details.others === true && details.other_drugs) {
						selectedDrugs.push(details.other_drugs)
					}
					return selectedDrugs.length > 0 ? selectedDrugs.join('、') : '无'
				} catch (e) {
					return drugAllergyDetails
				}
			},
			// 格式化疾病信息（JSON数组）
			formatDiseases(diseases) {
				if (!diseases) return ''
				try {
					const diseaseList = typeof diseases === 'string' ? JSON.parse(diseases) : diseases
					if (Array.isArray(diseaseList)) {
						return diseaseList.join('、')
					}
					return diseases
				} catch (e) {
					return diseases
				}
			},
			// 格式化残疾类型
			formatDisabilityTypes(disabilityTypes) {
				if (!disabilityTypes) return ''
				try {
					const types = typeof disabilityTypes === 'string' ? JSON.parse(disabilityTypes) : disabilityTypes
					if (Array.isArray(types)) {
						return types.join('、')
					}
					return disabilityTypes
				} catch (e) {
					return disabilityTypes
				}
			},
			// 获取优先级样式类
			getPriorityClass(priority) {
				if (priority === '低') return 'priority-low'
				if (priority === '高') return 'priority-high'
				return 'priority-medium'
			},
			// 获取建议类型文本
			getRecommendationTypeText(type) {
				const typeMap = {
					'LIFESTYLE': '生活方式',
					'DIET': '饮食',
					'EXERCISE': '运动',
					'MEDICAL': '医疗关注',
					'FOLLOW_UP': '随访',
					'SERVICE': '服务'
				}
				return typeMap[type] || type || ''
			},
			// 返回上一页
			goBack() {
				if (this.isEditMode) {
					uni.showModal({
						title: '提示',
						content: '您有未保存的修改，确定要退出吗？',
						success: (res) => {
							if (res.confirm) {
								this.isEditMode = false
								uni.navigateBack()
							}
						}
					})
				} else {
					uni.navigateBack()
				}
			},
			// 进入编辑模式
			enterEditMode() {
				this.isEditMode = true
				this.initEditData()
				// 编辑模式下展开所有section
				this.expandAllSections()
			},
			// 退出编辑模式（取消编辑）
			exitEditMode() {
				this.isEditMode = false
				// 收起所有section
				Object.keys(this.sectionExpanded).forEach(key => {
					this.$set(this.sectionExpanded, key, false)
				})
			},
			// 展开所有section
			expandAllSections() {
				Object.keys(this.sectionExpanded).forEach(key => {
					this.$set(this.sectionExpanded, key, true)
				})
			},
			// 切换section展开/收起状态
			toggleSection(sectionKey) {
				this.$set(this.sectionExpanded, sectionKey, !this.sectionExpanded[sectionKey])
			},
			// 判断section是否展开
			isSectionExpanded(sectionKey) {
				// 编辑模式下始终展开
				if (this.isEditMode) {
					return true
				}
				return this.sectionExpanded[sectionKey] || false
			},
			// 初始化编辑数据副本
			initEditData() {
				// 深拷贝数据，避免直接修改原数据
				this.editData = JSON.parse(JSON.stringify(this.detailData))
				// 初始化选择器选项数据
				if (!this.editData.healthProfileTags) {
					this.$set(this.editData, 'healthProfileTags', {})
				}
				if (!this.editData.userInfo) {
					this.$set(this.editData, 'userInfo', {})
				}
				if (!this.editData.userEmergencyContacts) {
					this.$set(this.editData, 'userEmergencyContacts', [])
				} else if (!Array.isArray(this.editData.userEmergencyContacts)) {
					// 如果是单个对象，转换为数组
					this.$set(this.editData, 'userEmergencyContacts', [this.editData.userEmergencyContacts])
				}
				if (!this.editData.userCertificates) {
					this.$set(this.editData, 'userCertificates', [])
				} else if (!Array.isArray(this.editData.userCertificates)) {
					// 如果是单个对象，转换为数组
					this.$set(this.editData, 'userCertificates', [this.editData.userCertificates])
				}
				if (!this.editData.allergyHistory) {
					this.$set(this.editData, 'allergyHistory', { allergyType: '无' })
				}
				if (!this.editData.exposureHistory) {
					this.$set(this.editData, 'exposureHistory', { exposureType: '无' })
				}
				if (!this.editData.diseaseHistory) {
					this.$set(this.editData, 'diseaseHistory', [])
				}
				if (!this.editData.vaccinationHistory) {
					this.$set(this.editData, 'vaccinationHistory', [])
				}
				if (!this.editData.familyHistory) {
					this.$set(this.editData, 'familyHistory', [])
				}
				if (!this.editData.geneticHistory) {
					this.$set(this.editData, 'geneticHistory', { hasGeneticDisease: '无' })
				}
				if (!this.editData.disabilityHistory) {
					this.$set(this.editData, 'disabilityHistory', {})
				}
				// 初始化慢性病和法定传染病文本
				if (this.chronicDiseaseList && this.chronicDiseaseList.length > 0) {
					this.editChronicDiseaseText = this.chronicDiseaseList.join('、')
				} else {
					this.editChronicDiseaseText = ''
				}
				if (this.statutoryInfoList && this.statutoryInfoList.length > 0) {
					this.editStatutoryInfoText = this.statutoryInfoList.join('、')
				} else {
					this.editStatutoryInfoText = ''
				}
				// 初始化药物过敏文本
				const drugAllergyDetails = this.safeGet(this.editData, 'allergyHistory.drugAllergyDetails')
				if (drugAllergyDetails) {
					try {
						const parsed = typeof drugAllergyDetails === 'string' 
							? JSON.parse(drugAllergyDetails)
							: drugAllergyDetails
						this.editDrugAllergyText = JSON.stringify(parsed, null, 2)
					} catch (e) {
						this.editDrugAllergyText = drugAllergyDetails || ''
					}
				} else {
					this.editDrugAllergyText = ''
				}
				// 初始化家族史疾病文本
				if (this.editData.familyHistory && this.editData.familyHistory.length > 0) {
					this.editFamilyDiseasesText = this.editData.familyHistory.map(family => {
						if (family.diseases) {
							try {
								const diseases = typeof family.diseases === 'string' ? JSON.parse(family.diseases) : family.diseases
								return Array.isArray(diseases) ? diseases.join('、') : ''
							} catch (e) {
								return ''
							}
						}
						return ''
					})
				} else {
					this.editFamilyDiseasesText = []
				}
				// 初始化残疾类型文本
				const disabilityTypes = this.safeGet(this.editData, 'disabilityHistory.disabilityTypes')
				if (disabilityTypes) {
					try {
						const types = typeof disabilityTypes === 'string' 
							? JSON.parse(disabilityTypes)
							: disabilityTypes
						this.editDisabilityTypesText = Array.isArray(types) ? types.join('、') : ''
					} catch (e) {
						this.editDisabilityTypesText = disabilityTypes || ''
					}
				} else {
					this.editDisabilityTypesText = ''
				}
			},
			// 获取选择器索引
			getPregnancyRiskIndex() {
				const pregnancyRisk = this.safeGet(this.editData, 'healthProfileTags.pregnancyRisk')
				if (!pregnancyRisk) return 0
				return this.pregnancyRiskOptions.indexOf(pregnancyRisk)
			},
			getWeightStatusIndex() {
				const weightStatus = this.safeGet(this.editData, 'healthProfileTags.weightStatus')
				if (!weightStatus) return 0
				return this.weightStatusOptions.indexOf(weightStatus)
			},
			getBloodTypeIndex() {
				const bloodType = this.safeGet(this.editData, 'healthProfileTags.bloodType')
				if (!bloodType) return 0
				return this.bloodTypeOptions.indexOf(bloodType)
			},
			// 选择器变化处理
			onPregnancyRiskChange(e) {
				this.updateField('healthProfileTags', 'pregnancyRisk', this.pregnancyRiskOptions[e.detail.value])
			},
			onWeightStatusChange(e) {
				this.updateField('healthProfileTags', 'weightStatus', this.weightStatusOptions[e.detail.value])
			},
			onBloodTypeChange(e) {
				this.updateField('healthProfileTags', 'bloodType', this.bloodTypeOptions[e.detail.value])
			},
			onPregnantChange(value) {
				this.updateField('healthProfileTags', 'isPregnant', value)
				if (!value) {
					this.updateField('healthProfileTags', 'pregnancyRisk', null)
				}
			},
			// 更新慢性病
			updateChronicDisease() {
				const diseases = this.editChronicDiseaseText.split('、').filter(d => d.trim())
				this.$set(this.editData.healthProfileTags, 'chronicDisease', JSON.stringify(diseases))
			},
			// 更新法定传染病
			updateStatutoryInfo() {
				const info = this.editStatutoryInfoText.split('、').filter(i => i.trim())
				this.$set(this.editData.healthProfileTags, 'statutoryInfo', JSON.stringify(info))
			},
			// 过敏史和暴露史选择器索引
			getAllergyTypeIndex() {
				const allergyType = this.safeGet(this.editData, 'allergyHistory.allergyType')
				if (!allergyType) return 0
				return this.allergyTypeOptions.indexOf(allergyType)
			},
			getExposureTypeIndex() {
				const exposureType = this.safeGet(this.editData, 'exposureHistory.exposureType')
				if (!exposureType) return 0
				return this.exposureTypeOptions.indexOf(exposureType)
			},
			// 过敏史和暴露史选择器变化处理
			onAllergyTypeChange(e) {
				this.updateField('allergyHistory', 'allergyType', this.allergyTypeOptions[e.detail.value])
			},
			onExposureTypeChange(e) {
				this.updateField('exposureHistory', 'exposureType', this.exposureTypeOptions[e.detail.value])
			},
			// 更新药物过敏
			updateDrugAllergy() {
				// 这里简化处理，实际应该解析JSON格式
				try {
					const parsed = JSON.parse(this.editDrugAllergyText)
					this.$set(this.editData.allergyHistory, 'drugAllergyDetails', JSON.stringify(parsed))
				} catch (e) {
					// 如果不是JSON格式，保存为字符串
					this.$set(this.editData.allergyHistory, 'drugAllergyDetails', this.editDrugAllergyText)
				}
			},
			// 添加/删除既往史
			addDiseaseHistory() {
				if (!this.editData.diseaseHistory) {
					this.$set(this.editData, 'diseaseHistory', [])
				}
				this.editData.diseaseHistory.push({ diseaseName: '', onsetDate: '' })
			},
			removeDiseaseHistory(index) {
				this.editData.diseaseHistory.splice(index, 1)
			},
			// 添加/删除预防接种史
			addVaccinationHistory() {
				if (!this.editData.vaccinationHistory) {
					this.$set(this.editData, 'vaccinationHistory', [])
				}
				this.editData.vaccinationHistory.push({ vaccineName: '', vaccinationDate: '' })
			},
			removeVaccinationHistory(index) {
				this.editData.vaccinationHistory.splice(index, 1)
			},
			// 添加/删除家族史
			addFamilyHistory() {
				if (!this.editData.familyHistory) {
					this.$set(this.editData, 'familyHistory', [])
				}
				this.editData.familyHistory.push({ relativeType: '', diseases: [] })
				this.editFamilyDiseasesText.push('')
			},
			removeFamilyHistory(index) {
				this.editData.familyHistory.splice(index, 1)
				this.editFamilyDiseasesText.splice(index, 1)
			},
			// 更新家族史疾病
			updateFamilyDiseases(index) {
				const diseases = this.editFamilyDiseasesText[index].split('、').filter(d => d.trim())
				this.$set(this.editData.familyHistory[index], 'diseases', JSON.stringify(diseases))
			},
			// 更新残疾类型
			updateDisabilityTypes() {
				const types = this.editDisabilityTypesText.split('、').filter(t => t.trim())
				this.$set(this.editData.disabilityHistory, 'disabilityTypes', JSON.stringify(types))
			},
			// 遗传病史选择器
			getGeneticDiseaseIndex() {
				const hasGeneticDisease = this.safeGet(this.editData, 'geneticHistory.hasGeneticDisease')
				if (!hasGeneticDisease) return 0
				return this.geneticDiseaseOptions.indexOf(hasGeneticDisease)
			},
			onGeneticDiseaseChange(e) {
				this.updateField('geneticHistory', 'hasGeneticDisease', this.geneticDiseaseOptions[e.detail.value])
				if (this.geneticDiseaseOptions[e.detail.value] === '无') {
					this.updateField('geneticHistory', 'diseaseName', null)
				}
			},
			// 家族史选择器
			getRelativeTypeIndex(relativeType) {
				if (!relativeType) return 0
				return this.relativeTypeOptions.indexOf(relativeType)
			},
			// 用户基本信息选择器索引
			getGenderIndex() {
				if (!this.safeGet(this.editData, 'userInfo.gender')) return 0
				return this.genderOptions.indexOf(this.editData.userInfo.gender)
			},
			getResidenceTypeIndex() {
				if (!this.safeGet(this.editData, 'userInfo.residenceType')) return 0
				return this.residenceTypeOptions.indexOf(this.editData.userInfo.residenceType)
			},
			getEducationLevelIndex() {
				if (!this.safeGet(this.editData, 'userInfo.educationLevel')) return 0
				return this.educationLevelOptions.indexOf(this.editData.userInfo.educationLevel)
			},
			getOccupationIndex() {
				if (!this.safeGet(this.editData, 'userInfo.occupation')) return 0
				return this.occupationOptions.indexOf(this.editData.userInfo.occupation)
			},
			getMaritalStatusIndex() {
				if (!this.safeGet(this.editData, 'userInfo.maritalStatus')) return 0
				return this.maritalStatusOptions.indexOf(this.editData.userInfo.maritalStatus)
			},
			getPaymentMethodIndex() {
				if (!this.safeGet(this.editData, 'userInfo.paymentMethod')) return 0
				return this.paymentMethodOptions.indexOf(this.editData.userInfo.paymentMethod)
			},
			// 用户基本信息选择器变化处理
			onGenderChange(e) {
				this.updateField('userInfo', 'gender', this.genderOptions[e.detail.value])
			},
			onResidenceTypeChange(e) {
				this.updateField('userInfo', 'residenceType', this.residenceTypeOptions[e.detail.value])
			},
			onEducationLevelChange(e) {
				this.updateField('userInfo', 'educationLevel', this.educationLevelOptions[e.detail.value])
			},
			onOccupationChange(e) {
				this.updateField('userInfo', 'occupation', this.occupationOptions[e.detail.value])
			},
			onMaritalStatusChange(e) {
				this.updateField('userInfo', 'maritalStatus', this.maritalStatusOptions[e.detail.value])
			},
			onPaymentMethodChange(e) {
				this.updateField('userInfo', 'paymentMethod', this.paymentMethodOptions[e.detail.value])
			},
			// 获取字段显示值（未填显示"未知"）
			getFieldValue(value, defaultValue) {
				// 如果没有指定默认值，使用"未知"
				if (defaultValue === undefined || defaultValue === null) {
					defaultValue = '未知'
				}
				// 检查各种空值情况
				if (value === null || value === undefined || value === '' || 
				    value === 'null' || value === 'undefined' || 
				    (typeof value === 'string' && value.trim() === '')) {
					return defaultValue
				}
				// 如果是数字0，应该显示0而不是"未知"
				if (typeof value === 'number' && value === 0) {
					return value
				}
				return value
			},
			// 判断字段是否为空
			isFieldEmpty(value) {
				return value === null || value === undefined || value === '' || value === 'null' || value === 'undefined'
			},
			// 安全获取嵌套属性值（避免在模板中使用可选链）
			safeGet(obj, path, defaultValue = null) {
				if (!obj) return defaultValue
				const keys = path.split('.')
				let result = obj
				for (const key of keys) {
					if (result === null || result === undefined) {
						return defaultValue
					}
					result = result[key]
				}
				return result === null || result === undefined ? defaultValue : result
			},
			// 检查嵌套字段是否为空
			isNestedFieldEmpty(obj, path) {
				const value = this.safeGet(obj, path)
				return this.isFieldEmpty(value)
			},
			// 编辑字段
			editField(section, field) {
				// 点击字段进入编辑状态（如果需要特殊处理）
			},
			// 处理行点击事件
			handleRowClick(section, field) {
				if (this.isEditMode) {
					this.editField(section, field)
				}
			},
			// 更新字段值
			updateField(section, field, value) {
				if (!this.editData[section]) {
					this.$set(this.editData, section, {})
				}
				this.$set(this.editData[section], field, value)
			},
			// 日期选择器变化
			onDateChange(section, field, e) {
				const value = e.detail.value
				this.updateField(section, field, value)
			},
			// 保存健康档案 - 使用各个表的独立更新接口（Vue2兼容版本）
			saveArchive() {
				uni.showLoading({
					title: '保存中...',
					mask: true
				})
				
				const userInfo = uni.getStorageSync('userInfo')
				const userId = userInfo ? userInfo.userId : null
				const token = uni.getStorageSync('token') || ''
				
				const updatePromises = []
				let hasError = false
				let errorMessage = ''
				
				// 1. 更新健康档案主表
				if (this.editData.healthArchive) {
					updatePromises.push(
						this.updateTable('health-archive-process', 'archive', this.editData.healthArchive, userId, token)
					)
				}
				
				// 2. 更新健康档案标签表
				if (this.editData.healthProfileTags) {
					// 处理慢性病和法定传染病JSON字段
					const tagsData = Object.assign({}, this.editData.healthProfileTags)
					if (this.editChronicDiseaseText) {
						const chronicList = this.editChronicDiseaseText.split(/[,，]/).map(function(s) { return s.trim() }).filter(function(s) { return s })
						tagsData.chronicDisease = JSON.stringify(chronicList)
					}
					if (this.editStatutoryInfoText) {
						const statutoryList = this.editStatutoryInfoText.split(/[,，]/).map(function(s) { return s.trim() }).filter(function(s) { return s })
						tagsData.statutoryInfo = JSON.stringify(statutoryList)
					}
					updatePromises.push(
						this.updateTable('health-profile-tags', 'archive', tagsData, userId, token)
					)
				}
				
				// 3. 更新用户基本信息表
				if (this.editData.userInfo) {
					updatePromises.push(
						this.updateTable('health-user-info', 'archive', this.editData.userInfo, userId, token)
					)
				}
				
				// 4. 更新紧急联系人表（一对多，需要传数组）
				if (this.editData.userEmergencyContacts) {
					const contacts = Array.isArray(this.editData.userEmergencyContacts) 
						? this.editData.userEmergencyContacts 
						: [this.editData.userEmergencyContacts]
					updatePromises.push(
						this.updateTableList('health-user-emergency-contacts', 'archive', contacts, userId, token)
					)
				}
				
				// 5. 更新健康服务凭证表（一对多，需要传数组）
				if (this.editData.userCertificates) {
					const certificates = Array.isArray(this.editData.userCertificates) 
						? this.editData.userCertificates 
						: [this.editData.userCertificates]
					updatePromises.push(
						this.updateTableList('health-user-certificates', 'archive', certificates, userId, token)
					)
				}
				
				// 6. 更新过敏史表
				if (this.editData.allergyHistory) {
					const allergyData = Object.assign({}, this.editData.allergyHistory)
					// 处理药物过敏详情JSON字段
					if (this.editDrugAllergyText) {
						try {
							allergyData.drugAllergyDetails = this.editDrugAllergyText
						} catch (e) {
							console.error('处理药物过敏详情失败', e)
						}
					}
					updatePromises.push(
						this.updateTable('health-allergy-history', 'archive', allergyData, userId, token)
					)
				}
				
				// 7. 更新暴露史表
				if (this.editData.exposureHistory) {
					updatePromises.push(
						this.updateTable('health-exposure-history', 'archive', this.editData.exposureHistory, userId, token)
					)
				}
				
				// 8. 更新既往史表（一对多，需要传数组）
				if (this.editData.diseaseHistory && Array.isArray(this.editData.diseaseHistory) && this.editData.diseaseHistory.length > 0) {
					updatePromises.push(
						this.updateTableList('health-disease-history', 'archive', this.editData.diseaseHistory, userId, token)
					)
				}
				
				// 9. 更新预防接种史表（一对多，需要传数组）
				if (this.editData.vaccinationHistory && Array.isArray(this.editData.vaccinationHistory) && this.editData.vaccinationHistory.length > 0) {
					updatePromises.push(
						this.updateTableList('health-vaccination-history', 'archive', this.editData.vaccinationHistory, userId, token)
					)
				}
				
				// 10. 更新家族史表（一对多，需要传数组）
				if (this.editData.familyHistory && Array.isArray(this.editData.familyHistory) && this.editData.familyHistory.length > 0) {
					// 处理diseases字段（确保是JSON字符串）
					const familyData = this.editData.familyHistory.map(function(family) {
						const familyItem = Object.assign({}, family)
						if (familyItem.diseases) {
							if (typeof familyItem.diseases === 'string') {
								// 已经是字符串，检查是否是JSON格式
								try {
									JSON.parse(familyItem.diseases)
								} catch (e) {
									// 不是JSON，转换为JSON
									familyItem.diseases = JSON.stringify([familyItem.diseases])
								}
							} else if (Array.isArray(familyItem.diseases)) {
								familyItem.diseases = JSON.stringify(familyItem.diseases)
							}
						}
						return familyItem
					})
					updatePromises.push(
						this.updateTableList('health-family-history', 'archive', familyData, userId, token)
					)
				}
				
				// 11. 更新遗传病史表
				if (this.editData.geneticHistory) {
					updatePromises.push(
						this.updateTable('health-genetic-history', 'archive', this.editData.geneticHistory, userId, token)
					)
				}
				
				// 12. 更新残疾情况表
				if (this.editData.disabilityHistory) {
					const disabilityData = Object.assign({}, this.editData.disabilityHistory)
					// 处理disabilityTypes字段（确保是JSON字符串）
					if (disabilityData.disabilityTypes) {
						if (typeof disabilityData.disabilityTypes === 'string') {
							try {
								JSON.parse(disabilityData.disabilityTypes)
							} catch (e) {
								disabilityData.disabilityTypes = JSON.stringify([disabilityData.disabilityTypes])
							}
						} else if (Array.isArray(disabilityData.disabilityTypes)) {
							disabilityData.disabilityTypes = JSON.stringify(disabilityData.disabilityTypes)
						}
					}
					updatePromises.push(
						this.updateTable('health-disability-status', 'archive', disabilityData, userId, token)
					)
				}
				
				// 执行所有更新请求（Vue2兼容：使用Promise.all和catch处理）
				const self = this
				Promise.all(updatePromises.map(function(promise) {
					return promise.catch(function(err) {
						return { error: err, code: 500 }
					})
				})).then(function(results) {
					// 检查是否有失败的请求
					results.forEach(function(result, index) {
						if (result.error) {
							hasError = true
							errorMessage = result.error.message || '部分数据保存失败'
							console.error('更新表 ' + index + ' 失败:', result.error)
						} else if (result && result.code !== 200) {
							hasError = true
							errorMessage = (result.message || '部分数据保存失败')
							console.error('更新表 ' + index + ' 失败:', result)
						}
					})
					
					uni.hideLoading()
					
					if (hasError) {
						uni.showToast({
							title: errorMessage || '部分数据保存失败',
							icon: 'none',
							duration: 3000
						})
					} else {
						uni.showToast({
							title: '保存成功',
							icon: 'success'
						})
						self.isEditMode = false
						// 收起所有section
						Object.keys(self.sectionExpanded).forEach(function(key) {
							self.$set(self.sectionExpanded, key, false)
						})
						// 重新加载数据
						self.loadArchiveDetail()
					}
				}).catch(function(error) {
					uni.hideLoading()
					console.error('保存健康档案失败:', error)
					uni.showToast({
						title: '保存失败，请稍后重试',
						icon: 'none'
					})
				})
			},
			
			// 更新单个表的辅助方法（一对一关系）
			updateTable(tablePath, pathParam, data, userId, token) {
				const self = this
				return new Promise(function(resolve, reject) {
					uni.request({
						url: config.baseUrl + '/' + tablePath + '/' + pathParam + '/' + self.archiveId,
						method: 'PUT',
						header: {
							'userId': userId ? userId.toString() : '',
							'token': token,
							'Content-Type': 'application/json'
						},
						data: data,
						success: function(res) {
							if (res.statusCode === 200 && res.data) {
								resolve(res.data)
							} else {
								reject(new Error((res.data && res.data.message) || '更新失败'))
							}
						},
						fail: function(err) {
							reject(err)
						}
					})
				})
			},
			
			// 更新列表表的辅助方法（一对多关系）
			updateTableList(tablePath, pathParam, dataList, userId, token) {
				const self = this
				return new Promise(function(resolve, reject) {
					uni.request({
						url: config.baseUrl + '/' + tablePath + '/' + pathParam + '/' + self.archiveId,
						method: 'PUT',
						header: {
							'userId': userId ? userId.toString() : '',
							'token': token,
							'Content-Type': 'application/json'
						},
						data: dataList,
						success: function(res) {
							if (res.statusCode === 200 && res.data) {
								resolve(res.data)
							} else {
								reject(new Error((res.data && res.data.message) || '更新失败'))
							}
						},
						fail: function(err) {
							reject(err)
						}
					})
				})
			},
			// 导出PDF
			exportToPdf() {
				if (!this.archiveId) {
					uni.showToast({
						title: '档案ID不能为空',
						icon: 'none'
					})
					return
				}

				uni.showLoading({
					title: '正在生成PDF...'
				})

				const token = uni.getStorageSync('token')
				const userInfo = uni.getStorageSync('userInfo')
				const userId = userInfo ? userInfo.userId : null

				// 获取档案名称作为文件名
				const archiveName = (this.detailData.healthArchive && this.detailData.healthArchive.archiveName) || 
				                   (this.detailData.healthArchive && this.detailData.healthArchive.userName) || 
				                   '健康档案'
				const fileName = `${archiveName}_${new Date().getTime()}.pdf`

				// 调用后端导出接口
				uni.request({
					url: `${config.baseUrl}/health-archive-process/export/pdf/${this.archiveId}`,
					method: 'GET',
					header: {
						'userId': userId || '',
						'token': token || ''
					},
					responseType: 'arraybuffer', // 接收二进制数据
					success: (res) => {
						uni.hideLoading()
						if (res.statusCode === 200) {
							// #ifdef H5
							// H5端：直接下载
							try {
								const blob = new Blob([res.data], { type: 'application/pdf' })
								const url = URL.createObjectURL(blob)
								const a = document.createElement('a')
								a.href = url
								a.download = fileName
								document.body.appendChild(a)
								a.click()
								document.body.removeChild(a)
								URL.revokeObjectURL(url)
								uni.showToast({
									title: '导出成功',
									icon: 'success'
								})
							} catch (e) {
								console.error('导出失败', e)
								uni.showToast({
									title: '导出失败',
									icon: 'none'
								})
							}
							// #endif

							// #ifdef MP-WEIXIN
							// 微信小程序：保存到临时文件并打开
							try {
								const fs = wx.getFileSystemManager()
								const filePath = `${wx.env.USER_DATA_PATH}/${fileName}`
								
								// 将ArrayBuffer写入文件
								fs.writeFileSync(filePath, res.data, 'binary')
								
								// 打开文档
								wx.openDocument({
									filePath: filePath,
									fileType: 'pdf',
									success: () => {
										uni.showToast({
											title: '打开成功',
											icon: 'success'
										})
									},
									fail: (err) => {
										console.error('打开文档失败', err)
										uni.showToast({
											title: '打开失败',
											icon: 'none'
										})
									}
								})
							} catch (e) {
								console.error('导出失败', e)
								uni.showToast({
									title: '导出失败',
									icon: 'none'
								})
							}
							// #endif

							// #ifdef APP-PLUS
							// APP端：保存到下载目录
							try {
								const blob = new Blob([res.data], { type: 'application/pdf' })
								const filePath = `_downloads/${fileName}`
								plus.io.resolveLocalFileSystemURL(filePath, (entry) => {
									entry.getFile(fileName, { create: true, exclusive: false }, (fileEntry) => {
										fileEntry.createWriter((writer) => {
											writer.write(blob)
											writer.onwriteend = () => {
												uni.showToast({
													title: '保存成功',
													icon: 'success'
												})
											}
											writer.onerror = (e) => {
												console.error('保存失败', e)
												uni.showToast({
													title: '保存失败',
													icon: 'none'
												})
											}
										})
									}, (e) => {
										console.error('创建文件失败', e)
										uni.showToast({
											title: '保存失败',
											icon: 'none'
										})
									})
								}, (e) => {
									console.error('访问下载目录失败', e)
									uni.showToast({
										title: '保存失败',
										icon: 'none'
									})
								})
							} catch (e) {
								console.error('导出失败', e)
								uni.showToast({
									title: '导出失败',
									icon: 'none'
								})
							}
							// #endif
						} else {
							// 尝试解析错误信息
							let errorMsg = '导出失败'
							try {
								const errorData = JSON.parse(new TextDecoder('utf-8').decode(res.data))
								errorMsg = errorData.message || errorMsg
							} catch (e) {
								// 无法解析错误信息，使用默认消息
							}
							uni.showToast({
								title: errorMsg,
								icon: 'none'
							})
						}
					},
					fail: (err) => {
						uni.hideLoading()
						console.error('导出PDF失败', err)
						uni.showToast({
							title: '网络错误，请稍后重试',
							icon: 'none'
						})
					}
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	.container {
		min-height: 100vh;
		background-color: #f5f5f5;
	}

	.loading-container {
		display: flex;
		justify-content: center;
		align-items: center;
		padding: 100rpx 0;
	}

	/* 自定义导航栏 */
	.custom-navbar {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 999;
		background-color: #ffffff;
		border-bottom: 1px solid #e5e5e5;
	}

	.navbar-content {
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 88rpx;
		padding: 0 32rpx;
		padding-top: var(--status-bar-height, 0);
	}

	.navbar-left {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 80rpx;
		height: 88rpx;
	}

	.navbar-title {
		flex: 1;
		text-align: center;
		font-size: 32rpx;
		font-weight: 500;
		color: #333333;
	}

	.navbar-right {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 80rpx;
		height: 88rpx;
	}

	.edit-btn, .save-btn {
		font-size: 28rpx;
		color: #07C160;
		font-weight: 500;
	}
	
	.save-btn {
		color: #1890ff;
	}

	.detail-scroll {
		height: calc(100vh - 88rpx);
		padding: 20rpx;
		padding-top: calc(88rpx + var(--status-bar-height, 0) + 20rpx);
	}

	.detail-section {
		margin-bottom: 32rpx;
	}

	.section-title {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 16rpx;
		padding: 16rpx 24rpx;
		background-color: #f8f8f8;
		border-radius: 12rpx;
		cursor: pointer;
		transition: background-color 0.2s;
		width: 100%;
		box-sizing: border-box;
	}
	
	.section-title:active {
		background-color: #eeeeee;
	}
	
	.title-left {
		display: flex;
		align-items: center;
		flex: 1;
		min-width: 0;
	}
	
	.title-right {
		display: flex;
		align-items: center;
		gap: 16rpx;
	}
	
	.collapse-arrow-wrapper {
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		margin-left: auto;
		padding-left: 16rpx;
		min-width: 40rpx;
		height: 40rpx;
	}
	
	.collapse-arrow {
		transition: transform 0.3s ease;
		display: block;
	}
	
	.add-btn {
		display: flex;
		align-items: center;
		gap: 8rpx;
		font-size: 24rpx;
		color: #07C160;
		padding: 8rpx 16rpx;
		background-color: rgba(7, 193, 96, 0.1);
		border-radius: 8rpx;
	}
	
	.delete-btn {
		margin-left: 16rpx;
		padding: 8rpx;
	}
	
	.empty-tip {
		padding: 32rpx;
		text-align: center;
	}

	.title-text {
		font-size: 32rpx;
		font-weight: 500;
		color: #333333;
		margin-left: 12rpx;
		flex: 1;
	}

	.info-card {
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 32rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.info-row {
		display: flex;
		margin-bottom: 24rpx;
		align-items: flex-start;
	}

	.info-row:last-child {
		margin-bottom: 0;
	}

	.info-label {
		font-size: 28rpx;
		color: #666666;
		min-width: 200rpx;
		flex-shrink: 0;
	}

	.info-value-wrapper {
		flex: 1;
	}
	
	.info-value {
		font-size: 28rpx;
		color: #333333;
		word-break: break-all;
	}
	
	.unknown-value {
		color: #999999;
		background-color: #f5f5f5;
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
	}
	
	.edit-input {
		flex: 1;
		font-size: 28rpx;
		color: #333333;
		background-color: #f5f5f5;
		border-radius: 8rpx;
		padding: 8rpx 16rpx;
		min-height: 60rpx;
	}
	
	.edit-textarea {
		flex: 1;
		font-size: 28rpx;
		color: #333333;
		background-color: #f5f5f5;
		border-radius: 8rpx;
		padding: 8rpx 16rpx;
		min-height: 120rpx;
	}
	
	.picker-view {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: space-between;
		background-color: #f5f5f5;
		border-radius: 8rpx;
		padding: 8rpx 16rpx;
		min-height: 60rpx;
	}
	
	.picker-text {
		font-size: 28rpx;
		color: #333333;
		
		&.placeholder {
			color: #999999;
		}
	}

	.health-score {
		font-size: 36rpx;
		font-weight: 600;
		color: #07C160;
	}

	.summary-text {
		line-height: 1.6;
	}

	.risk-low {
		color: #07C160;
		font-weight: 500;
	}

	.risk-medium {
		color: #FF9500;
		font-weight: 500;
	}

	.risk-high {
		color: #FF3B30;
		font-weight: 500;
	}

	.risk-very-high {
		color: #FF0000;
		font-weight: 600;
	}

	.recommendation-list {
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.recommendation-item {
		background-color: #ffffff;
		border-radius: 16rpx;
		padding: 32rpx;
		box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
	}

	.recommendation-header {
		display: flex;
		align-items: center;
		margin-bottom: 16rpx;
		gap: 16rpx;
	}

	.recommendation-priority {
		padding: 4rpx 16rpx;
		border-radius: 8rpx;
		font-size: 24rpx;
		font-weight: 500;
	}

	.priority-low {
		background-color: #E8F5E9;
		color: #07C160;
	}

	.priority-medium {
		background-color: #FFF3E0;
		color: #FF9500;
	}

	.priority-high {
		background-color: #FFEBEE;
		color: #FF3B30;
	}

	.recommendation-type {
		font-size: 24rpx;
		color: #999999;
		background-color: #F5F5F5;
		padding: 4rpx 12rpx;
		border-radius: 8rpx;
	}

	.recommendation-title {
		font-size: 32rpx;
		font-weight: 500;
		color: #333333;
		margin-bottom: 16rpx;
	}

	.recommendation-content {
		font-size: 28rpx;
		color: #666666;
		line-height: 1.8;
		white-space: pre-wrap;
		word-break: break-all;
	}

	/* 枚举字段样式 */
	.enum-field {
		margin-bottom: 32rpx;
	}

	.enum-field:last-child {
		margin-bottom: 0;
	}

	.field-label {
		font-size: 28rpx;
		color: #666666;
		font-weight: 500;
		margin-bottom: 16rpx;
	}

	.enum-options {
		display: flex;
		flex-wrap: wrap;
		gap: 16rpx;
		margin-top: 8rpx;
	}

	.enum-option {
		display: flex;
		align-items: center;
		margin-right: 24rpx;
		margin-bottom: 12rpx;
	}

	.enum-option checkbox {
		margin-right: 8rpx;
		transform: scale(0.9);
	}

	/* 自定义checkbox选中颜色为绿色 - 微信小程序 */
	/* #ifdef MP-WEIXIN */
	::v-deep .enum-option checkbox .wx-checkbox-input.wx-checkbox-input-checked {
		background-color: #07C160 !important;
		border-color: #07C160 !important;
	}

	::v-deep .enum-option checkbox .wx-checkbox-input.wx-checkbox-input-checked::before {
		color: #FFFFFF !important;
		font-size: 32rpx;
	}
	/* #endif */

	/* 自定义checkbox选中颜色为绿色 - uni-app H5/APP */
	/* #ifndef MP-WEIXIN */
	::v-deep .enum-option checkbox .uni-checkbox-input.uni-checkbox-input-checked {
		background-color: #07C160 !important;
		border-color: #07C160 !important;
	}

	::v-deep .enum-option checkbox .uni-checkbox-input.uni-checkbox-input-checked::before {
		color: #FFFFFF !important;
	}
	/* #endif */

	.option-text {
		font-size: 26rpx;
		color: #333333;
	}

	.field-value {
		font-size: 26rpx;
		color: #666666;
		margin-top: 8rpx;
		padding-left: 20rpx;
		line-height: 1.6;
	}

	/* 列表项样式 */
	.disease-item,
	.vaccination-item,
	.family-item {
		margin-bottom: 24rpx;
	}

	.disease-item:last-child,
	.vaccination-item:last-child,
	.family-item:last-child {
		margin-bottom: 0;
	}

	.disease-divider {
		height: 1rpx;
		background-color: #f0f0f0;
		margin: 24rpx 0;
	}
</style>
