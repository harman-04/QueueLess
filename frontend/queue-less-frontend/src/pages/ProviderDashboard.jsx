import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useParams, useNavigate } from "react-router-dom";
import QueueList from "../components/QueueList";
import CompletedTokensList from '../components/CompletedTokensList';
import EmergencyApprovalModal from '../components/EmergencyApprovalModal';
import UserDetailsModal from '../components/UserDetailsModal';
import ResetQueueModal from '../components/ResetQueueModal';
import ExportReportModal from '../components/ExportReportModal'; // NEW IMPORT for the modal
import { toast } from "react-toastify";
import "animate.css/animate.min.css";
import './ProviderDashboard.css';
import {
    FaTasks, FaSpinner, FaListAlt, FaPlayCircle, FaPauseCircle, FaSync,
    FaUsers, FaChartLine, FaClock, FaUserCheck, FaArrowLeft, FaWifi,
    FaHistory, FaExclamationTriangle, FaAmbulance, FaFilePdf, FaFileExcel,
    FaRedo, FaFileExport
} from "react-icons/fa";
import { Badge, Button, OverlayTrigger, Tooltip, Spinner } from "react-bootstrap";
import axios from "axios";
import WebSocketService from "../services/websocketService";

const axiosInstance = axios.create({
    baseURL: "https://localhost:8443/api",
});

const ProviderDashboard = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { queueId } = useParams();

    const { data: queueData, connected, error } = useSelector((state) => state.queue);
    const { token, role, name } = useSelector((state) => state.auth);
    const [updatingStatus, setUpdatingStatus] = useState(false);
    const [localQueueData, setLocalQueueData] = useState(null);
    const [connectionStatus, setConnectionStatus] = useState('disconnected');
    const [showEmergencyModal, setShowEmergencyModal] = useState(false);
    const [showUserDetailsModal, setShowUserDetailsModal] = useState(false);
    const [showResetModal, setShowResetModal] = useState(false);
    const [showExportModal, setShowExportModal] = useState(false); // NEW STATE for export modal
    const [selectedToken, setSelectedToken] = useState(null);
    const [exporting, setExporting] = useState(false);
    const [exportType, setExportType] = useState('');
    const [stats, setStats] = useState({
        waiting: 0,
        inService: 0,
        completed: 0,
        avgWaitTime: 0
    });

    useEffect(() => {
        if (queueData) {
            const normalizedQueue = {
                ...queueData,
                isActive: queueData.active !== undefined ? queueData.active : queueData.isActive
            };
            setLocalQueueData(normalizedQueue);

            const waiting = normalizedQueue.tokens?.filter(t => t.status === 'WAITING').length || 0;
            const inService = normalizedQueue.tokens?.filter(t => t.status === 'IN_SERVICE').length || 0;
            const completed = normalizedQueue.tokens?.filter(t => t.status === 'COMPLETED').length || 0;

            setStats({
                waiting,
                inService,
                completed,
                avgWaitTime: normalizedQueue.estimatedWaitTime || 0
            });
        }
    }, [queueData]);

    useEffect(() => {
        if (token && role === "PROVIDER" && queueId) {
            WebSocketService.connect();
            WebSocketService.subscribeToQueue(queueId);
            WebSocketService.subscribeToUserUpdates();

            setConnectionStatus('connecting');

            const fetchInitialQueueData = async () => {
                try {
                    const response = await axios.get(`${axiosInstance.defaults.baseURL}/queues/${queueId}`, {
                        headers: { Authorization: `Bearer ${token}` },
                    });
                    const normalizedQueue = {
                        ...response.data,
                        isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
                    };
                    setLocalQueueData(normalizedQueue);
                    setConnectionStatus('connected');
                } catch (err) {
                    console.error("Failed to fetch initial queue data:", err);
                    setConnectionStatus('error');
                }
            };

            fetchInitialQueueData();
        }

        return () => {
            WebSocketService.unsubscribeFromQueue(queueId);
        };
    }, [dispatch, token, role, queueId]);

    const handleToggleQueueStatus = async () => {
        if (!localQueueData) return;

        setUpdatingStatus(true);
        try {
            const endpoint = localQueueData.isActive
                ? `${axiosInstance.defaults.baseURL}/queues/${queueId}/deactivate`
                : `${axiosInstance.defaults.baseURL}/queues/${queueId}/activate`;

            const response = await axiosInstance.put(endpoint, {}, {
                headers: { Authorization: `Bearer ${token}` }
            });

            const updatedQueue = {
                ...response.data,
                isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
            };

            setLocalQueueData(updatedQueue);
            toast.success(`Queue ${localQueueData.isActive ? 'paused' : 'resumed'} successfully!`);
        } catch (error) {
            console.error("Error updating queue status:", error);
            if (error.response?.status === 500) {
                toast.error("Server error. Please try again later.");
            } else {
                toast.error("Failed to update queue status.");
            }
        } finally {
            setUpdatingStatus(false);
        }
    };

    const handleServeNext = () => {
        const success = WebSocketService.sendMessage("/app/queue/serve-next", { queueId });

        if (!success) {
            toast.error("Failed to send serve next request. Please try again.");
        }
    };

    const handleRefresh = () => {
        const fetchQueueData = async () => {
            try {
                const response = await axiosInstance.get(`${axiosInstance.defaults.baseURL}/queues/${queueId}`, {
                    headers: { Authorization: `Bearer ${token}` },
                });
                const normalizedQueue = {
                    ...response.data,
                    isActive: response.data.active !== undefined ? response.data.active : response.data.isActive
                };
                setLocalQueueData(normalizedQueue);
                toast.success("Queue data refreshed");
            } catch (error) {
                console.error("Failed to refresh queue data:", error);
                toast.error("Failed to refresh queue data");
            }
        };

        fetchQueueData();
    };

    const handleExport = async (format, reportType) => {
        setExporting(true);
        setExportType(`${format}-${reportType}`);
        const fileExtension = format === 'excel' ? 'xlsx' : format;

        try {
            const endpoint = `/export/queue/${queueId}/${format}?reportType=${reportType}&includeUserDetails=true`;

            const response = await axiosInstance.get(endpoint, {
                responseType: 'blob',
                headers: { Authorization: `Bearer ${token}` }
            });

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            const timestamp = new Date().getTime();
            const filename = `${localQueueData.serviceName}-${reportType}-${timestamp}.${fileExtension}`;
            link.setAttribute('download', filename);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
            toast.success('File exported successfully!');
        } catch (error) {
            console.error('Export error:', error);
            toast.error('Failed to export report');
        } finally {
            setExporting(false);
            setExportType('');
        }
    };

    const handleViewUserDetails = (tokenId) => {
        setSelectedToken(tokenId);
        setShowUserDetailsModal(true);
    };

    const handleResetQueue = async (options) => {
        try {
            const response = await axiosInstance.post(
                `/queues/${queueId}/reset-with-options`,
                options,
                { headers: { Authorization: `Bearer ${token}` } }
            );

            if (response.data.success) {
                toast.success('Queue reset successfully');

                if (response.data.exportFileUrl) {
                    try {
                        const fileResponse = await axiosInstance.get(response.data.exportFileUrl, {
                            responseType: 'blob',
                            headers: { Authorization: `Bearer ${token}` }
                        });

                        const url = window.URL.createObjectURL(new Blob([fileResponse.data]));
                        const link = document.createElement('a');
                        link.href = url;
                        link.setAttribute('download', `queue-reset-report-${new Date().getTime()}.pdf`);
                        document.body.appendChild(link);
                        link.click();
                        link.remove();
                        window.URL.revokeObjectURL(url);

                        toast.success('Export file downloaded successfully!');
                    } catch (downloadError) {
                        console.error('Error downloading export file:', downloadError);
                        toast.error('Failed to download the export file.');
                    }
                }
                handleRefresh();
            } else {
                toast.error('Failed to reset queue');
            }
        } catch (error) {
            console.error('Error resetting queue:', error);
            toast.error('Failed to reset queue');
        }
    };

    const isExporting = (format, reportType) =>
        exporting && exportType === `${format}-${reportType}`;

    if (!token) {
        return (
            <div className="d-flex justify-content-center align-items-center vh-100 text-primary">
                Please log in to view the dashboard.
            </div>
        );
    }

    if (role !== "PROVIDER") {
        return (
            <div className="d-flex justify-content-center align-items-center vh-100 text-danger">
                Access Denied. You must be logged in as a provider.
            </div>
        );
    }

    const queue = localQueueData;
    const pendingEmergencyCount = queue?.pendingEmergencyTokens?.length || 0;
    const completedTokens = queue?.tokens?.filter(t => t.status === 'COMPLETED') || [];

    const renderTooltip = (tooltipText) => (
        <Tooltip id={`tooltip-${tooltipText.replace(/\s/g, '-')}`}>
            {tooltipText}
        </Tooltip>
    );

    return (
        <div className="provider-dashboard-container">
            <div className="main-dashboard-content">
                {/* Header */}
                <div className="provider-dashboard-header animate__animated animate__fadeInDown">
                    <div className="provider-dashboard-header-content">
                        <button
                            onClick={() => navigate("/provider/queues")}
                            className="provider-dashboard-back-button"
                        >
                            <FaArrowLeft className="me-2" /> All Queues
                        </button>

                        <div className="provider-dashboard-queue-title-section">
                            <div className="provider-dashboard-queue-icon">
                                <FaTasks />
                            </div>
                            <div className="provider-dashboard-queue-info">
                                <h1 className="provider-dashboard-queue-name">
                                    {localQueueData ? localQueueData.serviceName : "Loading Queue..."}
                                </h1>
                                <div className="provider-dashboard-queue-meta">
                                    <span className="provider-dashboard-queue-id">ID: {queueId}</span>
                                    <div className="provider-dashboard-queue-status">
                                        <span className={`provider-dashboard-status-badge ${localQueueData?.isActive ? 'active' : 'paused'}`}>
                                            {localQueueData?.isActive ? 'Active' : 'Paused'}
                                        </span>
                                        <div className="provider-dashboard-connection-status">
                                            <div className={`provider-dashboard-connection-indicator ${connectionStatus}`}>
                                                {connectionStatus === 'connected' ? <FaWifi /> : <FaExclamationTriangle />}
                                            </div>
                                            <span className="provider-dashboard-status-text">
                                                {connectionStatus === 'connected' ? 'Live Connected' :
                                                    connectionStatus === 'connecting' ? 'Connecting...' :
                                                        connectionStatus === 'error' ? 'Connection Error' : 'Disconnected'}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="provider-dashboard-header-actions">
                            {/* Primary Actions */}
                            {localQueueData && (
                                <>
                                    <OverlayTrigger placement="bottom" overlay={renderTooltip(localQueueData.isActive ? 'Pause Queue' : 'Resume Queue')}>
                                        <button
                                            onClick={handleToggleQueueStatus}
                                            className={`provider-dashboard-action-btn provider-dashboard-status-toggle-btn ${localQueueData.isActive ? 'pause' : 'resume'}`}
                                            disabled={updatingStatus}
                                        >
                                            {updatingStatus ? <FaSpinner className="provider-dashboard-spinning" /> : localQueueData.isActive ? <FaPauseCircle /> : <FaPlayCircle />}
                                            <span>{localQueueData.isActive ? 'Pause Queue' : 'Resume Queue'}</span>
                                        </button>
                                    </OverlayTrigger>

                                    <OverlayTrigger placement="bottom" overlay={renderTooltip('Serve Next Token')}>
                                        <button onClick={handleServeNext} className="provider-dashboard-action-btn provider-dashboard-serve-next-btn">
                                            Serve Next <FaPlayCircle className="ms-2" />
                                        </button>
                                    </OverlayTrigger>
                                </>
                            )}

                            {/* Secondary Actions (Grouped) */}
                            <div className="provider-dashboard-secondary-actions">
                                <OverlayTrigger placement="bottom" overlay={renderTooltip('Refresh queue data')}>
                                    <button onClick={handleRefresh} className="provider-dashboard-action-btn provider-dashboard-icon-btn">
                                        <FaSync />
                                    </button>
                                </OverlayTrigger>

                                <OverlayTrigger placement="bottom" overlay={renderTooltip('View all queues')}>
                                    <button onClick={() => navigate("/provider/queues")} className="provider-dashboard-action-btn provider-dashboard-icon-btn">
                                        <FaListAlt />
                                    </button>
                                </OverlayTrigger>

                                <OverlayTrigger placement="bottom" overlay={renderTooltip('Emergency Approvals')}>
                                    <Button
                                        variant="warning"
                                        onClick={() => setShowEmergencyModal(true)}
                                        className="provider-dashboard-action-btn provider-dashboard-icon-btn provider-dashboard-emergency-btn"
                                    >
                                        <FaAmbulance />
                                        {pendingEmergencyCount > 0 && <Badge bg="danger" className="ms-1">{pendingEmergencyCount}</Badge>}
                                    </Button>
                                </OverlayTrigger>

                                {/* Export Reports Button */}
                                <OverlayTrigger placement="bottom" overlay={renderTooltip('Export Reports')}>
                                    <Button
                                        variant="primary"
                                        onClick={() => setShowExportModal(true)} // Open the modal
                                        className="provider-dashboard-action-btn provider-dashboard-icon-btn provider-dashboard-export-btn"
                                    >
                                        <FaFileExport />
                                    </Button>
                                </OverlayTrigger>

                                <OverlayTrigger placement="bottom" overlay={renderTooltip('Reset Queue with Options')}>
                                    <Button onClick={() => setShowResetModal(true)} variant="danger" className="provider-dashboard-action-btn provider-dashboard-icon-btn provider-dashboard-reset-btn">
                                        <FaRedo />
                                    </Button>
                                </OverlayTrigger>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Stats Cards */}
                <div className="provider-dashboard-stats-grid animate__animated animate__fadeIn" >
                    <div className="provider-dashboard-stat-card provider-dashboard-waiting">
                        <div className="provider-dashboard-stat-icon">
                            <FaUsers />
                        </div>
                        <div className="provider-dashboard-stat-content">
                            <h3 className="provider-dashboard-stat-value">{stats.waiting}</h3>
                            <p className="provider-dashboard-stat-label">Waiting</p>
                        </div>
                        <div className="provider-dashboard-stat-trend">
                            <FaChartLine className="provider-dashboard-trend-up" />
                        </div>
                    </div>

                    <div className="provider-dashboard-stat-card provider-dashboard-in-service">
                        <div className="provider-dashboard-stat-icon">
                            <FaUserCheck />
                        </div>
                        <div className="provider-dashboard-stat-content">
                            <h3 className="provider-dashboard-stat-value">{stats.inService}</h3>
                            <p className="provider-dashboard-stat-label">In Service</p>
                        </div>
                    </div>

                    <div className="provider-dashboard-stat-card provider-dashboard-completed">
                        <div className="provider-dashboard-stat-icon">
                            <FaHistory />
                        </div>
                        <div className="provider-dashboard-stat-content">
                            <h3 className="provider-dashboard-stat-value">{stats.completed}</h3>
                            <p className="provider-dashboard-stat-label">Completed</p>
                        </div>
                    </div>

                    <div className="provider-dashboard-stat-card provider-dashboard-wait-time">
                        <div className="provider-dashboard-stat-icon">
                            <FaClock />
                        </div>
                        <div className="provider-dashboard-stat-content">
                            <h3 className="provider-dashboard-stat-value">{stats.avgWaitTime}m</h3>
                            <p className="provider-dashboard-stat-label">Avg. Wait Time</p>
                        </div>
                    </div>
                </div>

                {/* Status Messages */}
                {connectionStatus === 'error' && (
                    <div className="provider-dashboard-status-alert provider-dashboard-error animate__animated animate__fadeIn">
                        <FaExclamationTriangle className="me-2" />
                        Connection error. Please check your internet connection and try refreshing.
                    </div>
                )}

                {!connected && connectionStatus === 'connecting' && (
                    <div className="provider-dashboard-status-alert provider-dashboard-warning animate__animated animate__fadeIn">
                        <FaSpinner className="provider-dashboard-spinning me-2" />
                        Connecting to live queue...
                    </div>
                )}

                {/* Queue List */}
                <div className="provider-dashboard-queue-list-container animate__animated animate__fadeIn">
                    {localQueueData ? (
                        <>
                            <QueueList
                                queue={localQueueData}
                                onServeNext={handleServeNext}
                                onViewUserDetails={handleViewUserDetails}
                            />
                            <CompletedTokensList completedTokens={completedTokens} />
                        </>
                    ) : (
                        <div className="provider-dashboard-no-queue-message">
                            <div className="provider-dashboard-loading-spinner">
                                <FaSpinner className="provider-dashboard-spinning" size="2rem" />
                            </div>
                            <h3>Loading Queue Data</h3>
                            <p>Please wait while we load your queue information</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Modals */}
            <EmergencyApprovalModal
                show={showEmergencyModal}
                onHide={() => setShowEmergencyModal(false)}
                queueId={queueId}
            />

            <UserDetailsModal
                show={showUserDetailsModal}
                onHide={() => setShowUserDetailsModal(false)}
                queueId={queueId}
                tokenId={selectedToken}
            />

            <ResetQueueModal
                show={showResetModal}
                onHide={() => setShowResetModal(false)}
                onReset={handleResetQueue}
                queueName={localQueueData?.serviceName}
            />

            {/* NEW EXPORT MODAL */}
            <ExportReportModal
                show={showExportModal}
                onHide={() => setShowExportModal(false)}
                onExport={handleExport}
                isExporting={isExporting}
            />
        </div>
    );
};

export default ProviderDashboard;